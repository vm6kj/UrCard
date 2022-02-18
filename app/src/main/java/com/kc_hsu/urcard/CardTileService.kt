package com.kc_hsu.urcard

import android.content.Context
import android.util.DisplayMetrics
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.expand
import androidx.wear.tiles.LayoutElementBuilders.Box
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.FontStyles
import androidx.wear.tiles.LayoutElementBuilders.Image
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement
import androidx.wear.tiles.LayoutElementBuilders.Spacer
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.StateBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.material.CompactChip
import com.google.common.util.concurrent.ListenableFuture
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import java.util.EnumMap
import kotlin.math.roundToInt

private val VERTICAL_SPACING_HEIGHT = dp(8f)
private val BARCODE_TEXT_HEIGHT = dp(24f)
private val BARCODE_IMAGE_HEIGHT = 48f
private val PADDING = 12f

class CardTileService : TileService() {
    private val RESOURCES_VERSION = "1"
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val ID_IMAGE_FROM_RESOURCE = "image_from_resource"

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> =
        serviceScope.future {
            val deviceParams = requestParams.deviceParameters!!

            Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTimeline(
                    Timeline.Builder()
                        .addTimelineEntry(
                            TimelineEntry.Builder()
                                .setLayout(
                                    Layout.Builder().setRoot(
                                        layout(deviceParams)
                                    )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        }

    private fun layout(deviceParams: DeviceParameters): LayoutElement =
        Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setVerticalAlignment(VERTICAL_ALIGN_BOTTOM)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setTop(dp(PADDING))
                            .setBottom(dp(PADDING))
                            .build()
                    )
                    .build()
            )
            .addContent(
                Column.Builder()
                    .setWidth(expand())
                    .addContent(
                        barcodeImage()
                    )
                    .addContent(Spacer.Builder().setHeight(VERTICAL_SPACING_HEIGHT).build())
                    .addContent(
                        // TODO retrieve from repo
                        barcodeInText("/AM1172S", deviceParams)
                    )
                    .addContent(Spacer.Builder().setHeight(VERTICAL_SPACING_HEIGHT).build())
                    .addContent(
                        addMore(deviceParams)
                    )
                    .build()
            )
            .build()

    private fun addMore(deviceParams: DeviceParameters): LayoutElement =
        CompactChip.Builder(
            getString(R.string.more_card),
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setClassName(MainActivity::class.java.name)
                        .setPackageName(this.packageName)
                        .build()
                )
                .build(),
            "addMore",
            deviceParams
        )
            .build()

    private fun barcodeImage(): LayoutElement =
        Image.Builder()
            .setWidth(expand())
            .setHeight(dp(BARCODE_IMAGE_HEIGHT))
            .setResourceId(ID_IMAGE_FROM_RESOURCE)
            .build()

    private fun barcodeInText(barcode: String, deviceParameters: DeviceParameters): LayoutElement =
        Text.Builder()
            .setText(barcode)
            .setFontStyle(FontStyles.body1(deviceParameters).build())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId("refresh")
                            .setOnClick(
                                ActionBuilders.LoadAction.Builder()
                                    .setRequestState(
                                        StateBuilders.State.Builder()
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        serviceScope.future {
            val multiFormatWriter = MultiFormatWriter()
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 50
            val bitMatrix = multiFormatWriter.encode(
                "/AM1172S",
                BarcodeFormat.CODE_39,
                requestParams.deviceParameters?.screenWidthDp ?: 0,
                (requestParams.deviceParameters?.screenHeightDp ?: 0),
                hints
            )
            val barcodeEncoder = BarcodeEncoder()

            val barcodeBitmap = barcodeEncoder.createBitmap(bitMatrix)

            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .addIdToImageMapping(
                    ID_IMAGE_FROM_RESOURCE,
                    ResourceBuilders.ImageResource.Builder()
                        .setInlineResource(
                            ResourceBuilders.InlineImageResource.Builder()
                                .setData(barcodeBitmap.toByteArray())
                                .setWidthPx(
                                    convertDpToPixel(
                                        requestParams.deviceParameters?.screenWidthDp!!.toFloat(),
                                        this@CardTileService
                                    ).roundToInt()
                                )
                                // .setHeightPx(convertDpToPixel(requestParams.deviceParameters?.screenHeightDp!!.toFloat(), this@CardTileService).roundToInt() / 4)
                                .setHeightPx((BARCODE_IMAGE_HEIGHT * requestParams.deviceParameters!!.screenDensity).roundToInt())
                                .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
                                .build()
                        )
                        .build()
                )
                .build()
        }

    private fun getDensity(context: Context): Float {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        return metrics.density
    }

    private fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * getDensity(context)
    }
}