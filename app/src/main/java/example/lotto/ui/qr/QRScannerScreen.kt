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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScanScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    // 중복 실행 방지 플래그
    var isScanned by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasCameraPermission = it }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF8FAFC)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("QR 당첨 확인", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(32.dp))

            if (hasCameraPermission) {
                Box(modifier = Modifier.size(280.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black).border(BorderStroke(2.dp, Color(0xFF0EA5E9)), RoundedCornerShape(24.dp))) {
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
                                                                isScanned = true // 최초 1회만 실행되도록 잠금
                                                                try {
                                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                                    ctx.startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            }
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
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "카메라 권한이 필요합니다",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("QR을 인식하면 자동으로 당첨 확인 페이지가 열립니다.", fontSize = 14.sp, color = Color(0xFF64748B))
        }
    }
}
