// File Path: app/src/main/java/com/kimro/ai/lotto/ui/qr/QrScanScreen.kt
package com.kimro.ai.lotto.ui.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    var isScanned by remember { mutableStateOf(false) }

    // 외부 브라우저(동행복권 당첨 페이지)로 갔다가 뒤로가기로 돌아왔을 때, 화면이 다시 보여지는
    // 시점(ON_RESUME)마다 isScanned를 초기화해서 다음 QR도 바로 인식되도록 한다.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isScanned = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasCameraPermission = it }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI로또 6/45",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                fontSize = 21.sp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED))
                                )
                            )
                        )
                        Text(
                            text = "QR 당첨 확인",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 카메라 미리보기 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📷", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "로또 용지 QR을 비춰주세요",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black)
                    ) {
                        if (hasCameraPermission) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    val scanner = BarcodeScanning.getClient()
                                    val executor = Executors.newSingleThreadExecutor()

                                    cameraProviderFuture.addListener({
                                        try {
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }

                                            val imageAnalysis = ImageAnalysis.Builder()
                                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                .build()
                                                .also { analysis ->
                                                    analysis.setAnalyzer(executor) { imageProxy ->
                                                        if (isScanned) {
                                                            imageProxy.close()
                                                            return@setAnalyzer
                                                        }

                                                        val mediaImage = imageProxy.image
                                                        if (mediaImage != null) {
                                                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                                            scanner.process(image)
                                                                .addOnSuccessListener { barcodes ->
                                                                    val url = barcodes.firstOrNull()?.rawValue
                                                                    if (!url.isNullOrEmpty() && !isScanned) {
                                                                        isScanned = true
                                                                        try {
                                                                            // 안전한 애플리케이션 컨텍스트 및 새 태스크 플래그 추가로 튕김 방지
                                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                            }
                                                                            context.applicationContext.startActivity(intent)
                                                                        } catch (e: Exception) {
                                                                            e.printStackTrace()
                                                                            isScanned = false // 실패 시 다시 스캔 가능하도록 복구
                                                                        }
                                                                    }
                                                                }
                                                                .addOnFailureListener {
                                                                    // 무시
                                                                }
                                                                .addOnCompleteListener {
                                                                    imageProxy.close()
                                                                }
                                                        } else {
                                                            imageProxy.close()
                                                        }
                                                    }
                                                }

                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                imageAnalysis
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))

                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // 스캔 가이드 프레임 (모서리 브라켓 장식)
                            ScanCornerFrame(modifier = Modifier.fillMaxSize())
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "🔒", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "카메라 권한이 필요합니다",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("권한 허용하기", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 사용법 안내 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "이렇게 사용하세요",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    GuideStepRow(step = "1", text = "로또 용지 뒷면의 QR 코드를 사각형 안에 맞춰주세요")
                    Spacer(modifier = Modifier.height(10.dp))
                    GuideStepRow(step = "2", text = "인식되면 자동으로 동행복권 당첨 확인 페이지로 이동해요")
                    Spacer(modifier = Modifier.height(10.dp))
                    GuideStepRow(step = "3", text = "여러 장 확인할 땐, 뒤로가기 후 다음 용지를 다시 비춰주세요")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/** 카메라 미리보기 위에 얹는 모서리 브라켓 장식 - 일반적인 QR 스캐너 UI 느낌을 준다. */
@Composable
private fun ScanCornerFrame(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(20.dp)) {
        val bracketColor = Color(0xFF0EA5E9)
        val thickness = 4.dp
        val length = 32.dp

        // 좌상단
        Box(Modifier.align(Alignment.TopStart)) {
            Box(Modifier.size(length, thickness).background(bracketColor))
            Box(Modifier.size(thickness, length).background(bracketColor))
        }
        // 우상단
        Box(Modifier.align(Alignment.TopEnd)) {
            Box(Modifier.align(Alignment.TopEnd).size(length, thickness).background(bracketColor))
            Box(Modifier.align(Alignment.TopEnd).size(thickness, length).background(bracketColor))
        }
        // 좌하단
        Box(Modifier.align(Alignment.BottomStart)) {
            Box(Modifier.align(Alignment.BottomStart).size(length, thickness).background(bracketColor))
            Box(Modifier.align(Alignment.BottomStart).size(thickness, length).background(bracketColor))
        }
        // 우하단
        Box(Modifier.align(Alignment.BottomEnd)) {
            Box(Modifier.align(Alignment.BottomEnd).size(length, thickness).background(bracketColor))
            Box(Modifier.align(Alignment.BottomEnd).size(thickness, length).background(bracketColor))
        }
    }
}

@Composable
private fun GuideStepRow(step: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED)))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            lineHeight = 18.sp
        )
    }
}
