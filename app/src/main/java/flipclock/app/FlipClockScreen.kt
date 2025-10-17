package flipclock.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FlipClockScreen(modifier: Modifier = Modifier) {
    var dateTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            dateTime = now
            val millisUntilNextSecond = 1000 - now.nano / 1_000_000
            delay(millisUntilNextSecond.toLong())
        }
    }

    val hour24 = dateTime.hour
    val minute = dateTime.minute
    val second = dateTime.second
    val hour12 = ((hour24 + 11) % 12) + 1
    val amPm = if (hour24 >= 12) "PM" else "AM"
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    }
    val dateLabel = dateTime.format(dateFormatter)

    val digitTextStyle = MaterialTheme.typography.displayMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 76.sp,
        letterSpacing = (-1.5).sp
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF000000)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF131313), Color(0xFF020202))
                    )
                )
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFA6A6A6)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = amPm,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp
                    ),
                    color = Color(0xFFFCFCFC)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hourTens = hour12 / 10
                val hourOnes = hour12 % 10
                val minuteTens = minute / 10
                val minuteOnes = minute % 10

                FlipDigit(
                    value = hourTens.takeIf { hour12 >= 10 },
                    textStyle = digitTextStyle
                )
                FlipDigit(value = hourOnes, textStyle = digitTextStyle)
                BlinkingColon(isVisible = second % 2 == 0)
                FlipDigit(value = minuteTens, textStyle = digitTextStyle)
                FlipDigit(value = minuteOnes, textStyle = digitTextStyle)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BlinkingColon(isVisible: Boolean, modifier: Modifier = Modifier) {
    val targetColor = if (isVisible) Color(0xFFFCFCFC) else Color(0x33FFFFFF)
    val color by animateColorAsState(targetValue = targetColor, animationSpec = tween(350), label = "colonColor")
    Text(
        text = ":",
        modifier = modifier.padding(bottom = 8.dp),
        style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 64.sp
        ),
        color = color,
        textAlign = TextAlign.Center
    )
}

private enum class DigitHalf { Top, Bottom }

@Composable
private fun FlipDigit(
    value: Int?,
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    cardColor: Color = Color(0xFF1B1B1F),
    topHighlight: Color = Color(0xFF2A2A30),
    bottomShadow: Color = Color(0xFF121216)
) {
    val progress = remember { Animatable(1f) }
    var currentDigit by remember { mutableStateOf(value) }
    var previousDigit by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value != currentDigit) {
            previousDigit = currentDigit
            currentDigit = value
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = CubicBezierEasing(0.2f, 0.55f, 0.24f, 1f)
                )
            )
        }
    }

    val fraction = progress.value
    val isFlipping = previousDigit != currentDigit && fraction < 1f
    val displayDigit = if (fraction < 0.5f) previousDigit else currentDigit
    val cameraDistance = 52.dp

    BoxWithConstraints(
        modifier = modifier
            .size(width = 96.dp, height = 128.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
    ) {
        val halfHeight = maxHeight / 2
        Column(modifier = Modifier.matchParentSize()) {
            DigitStaticHalf(
                digit = displayDigit,
                textStyle = textStyle,
                color = topHighlight,
                half = DigitHalf.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(halfHeight)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x33000000))
            )
            DigitStaticHalf(
                digit = displayDigit,
                textStyle = textStyle,
                color = bottomShadow,
                half = DigitHalf.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(halfHeight)
            )
        }

        if (isFlipping && fraction < 0.5f) {
            val rotation = -180f * fraction
            DigitFlipHalf(
                digit = previousDigit,
                textStyle = textStyle,
                color = topHighlight,
                half = DigitHalf.Top,
                rotationX = rotation,
                shadeAlpha = fraction * 0.55f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(halfHeight)
                    .align(Alignment.TopCenter),
                cameraDistance = cameraDistance
            )
        }

        if (isFlipping && fraction >= 0.5f) {
            val rotation = 180f * (1f - fraction)
            DigitFlipHalf(
                digit = currentDigit,
                textStyle = textStyle,
                color = bottomShadow,
                half = DigitHalf.Bottom,
                rotationX = rotation,
                shadeAlpha = (1f - fraction) * 0.55f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(halfHeight)
                    .align(Alignment.BottomCenter),
                cameraDistance = cameraDistance
            )
        }
    }
}

@Composable
private fun DigitStaticHalf(
    digit: Int?,
    textStyle: TextStyle,
    color: Color,
    half: DigitHalf,
    modifier: Modifier = Modifier
) {
    val shape = when (half) {
        DigitHalf.Top -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        DigitHalf.Bottom -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit?.toString() ?: "",
            style = textStyle,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DigitFlipHalf(
    digit: Int?,
    textStyle: TextStyle,
    color: Color,
    half: DigitHalf,
    rotationX: Float,
    shadeAlpha: Float,
    modifier: Modifier,
    cameraDistance: Dp
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pxCameraDistance = with(density) { cameraDistance.toPx() }
    val shape = when (half) {
        DigitHalf.Top -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        DigitHalf.Bottom -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationX = rotationX
                transformOrigin = TransformOrigin(0.5f, if (half == DigitHalf.Top) 1f else 0f)
                this.cameraDistance = pxCameraDistance
            }
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit?.toString() ?: "",
            style = textStyle,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = when (half) {
                        DigitHalf.Top -> Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = shadeAlpha), Color.Transparent)
                        )
                        DigitHalf.Bottom -> Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = shadeAlpha))
                        )
                    }
                )
        )
    }
}

@Preview
@Composable
private fun FlipClockScreenPreview() {
    MaterialTheme {
        FlipClockScreen()
    }
}
