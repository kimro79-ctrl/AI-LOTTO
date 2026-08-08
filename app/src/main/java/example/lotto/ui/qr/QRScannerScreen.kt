// File Path: app/src/main/java/com/kimro/ai/lotto/ui/qr/QrScanScreen.kt
package com.kimro.ai.lotto.ui.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScanScreen(
    qrScanViewModel: QrScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    var isScanned by remember { mutableStateOf(false) }

    val savedCount by qrScanViewModel.savedCount.collectAsState()
    val saveMessage by qrScanViewModel.saveMessage.collectAsState()

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            qrScanViewModel.clearSaveMessage()
        }
    }

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

            if (savedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFE0F2FE),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "내역에 저장된 게임 수: ${savedCount}개",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                                                                isScanned = true

                                                                // 동행복권 QR이면 회차+번호를 파싱해서 히스토리에 먼저 저장
                                                                if (url.contains("dhlottery.co.kr")) {
                                                                    val games = parseAllLottoGamesFromUrl(url)
                                                                    if (games.isNotEmpty()) {
                                                                        qrScanViewModel.saveScannedGames(games)
                                                                    }
                                                                }

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
            Text("QR을 인식하면 번호가 자동 저장되고, 당첨 확인 페이지가 열립니다.", fontSize = 14.sp, color = Color(0xFF64748B))
        }
    }
}

/**
 * 동행복권 QR URL에서 (회차, 번호6개) 조합들을 파싱한다.
 * 용지 하나에 최대 5게임까지 들어있을 수 있어 여러 개를 반환할 수 있다.
 */
private fun parseAllLottoGamesFromUrl(url: String): List<Pair<Int, List<Int>>> {
    val results = mutableListOf<Pair<Int, List<Int>>>()
    try {
        val uri = Uri.parse(url)
        val vParam = uri.getQueryParameter("v") ?: return results

        val tokens = vParam.split("q", "and", ",")

        for (token in tokens) {
            val parts = token.split("m")
            if (parts.size >= 2) {
                val round = parts[0].toIntOrNull() ?: continue
                val numbersStr = parts[1]

                val allNumbers = mutableListOf<Int>()
                var i = 0
                while (i < numbersStr.length - 1) {
                    val numStr = numbersStr.substring(i, i + 2)
                    numStr.toIntOrNull()?.let { allNumbers.add(it) }
                    i += 2
                }

                if (allNumbers.size >= 6) {
                    for (chunk in allNumbers.chunked(6)) {
                        if (chunk.size == 6) {
                            results.add(Pair(round, chunk))
                        }
                    }
                }
            }
        }

        if (results.isEmpty()) {
            val mainParts = vParam.split("m")
            if (mainParts.size >= 2) {
                val round = mainParts[0].toIntOrNull() ?: return results
                val numbersStr = mainParts[1]
                val allNumbers = mutableListOf<Int>()
                var i = 0
                while (i < numbersStr.length - 1) {
                    val numStr = numbersStr.substring(i, i + 2)
                    numStr.toIntOrNull()?.let { allNumbers.add(it) }
                    i += 2
                }
                for (chunk in allNumbers.chunked(6)) {
                    if (chunk.size == 6) {
                        results.add(Pair(round, chunk))
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return results.distinct()
}
