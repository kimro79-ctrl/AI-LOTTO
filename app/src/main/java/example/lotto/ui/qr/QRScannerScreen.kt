package com.kimro.ai.lotto.ui.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kimro.ai.lotto.ui.purchase.PurchaseViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    purchaseViewModel: PurchaseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "QR 당첨 확인",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFE2E8F0))
                        .border(2.dp, Color(0xFF0EA5E9), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                val executor = Executors.newSingleThreadExecutor()

                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    
                                    val scanner = BarcodeScanning.getClient()
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        
                                    var isScanned = false

                                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                        processImageProxy(scanner, imageProxy) { qrResultUrl ->
                                            if (!isScanned && qrResultUrl.contains("dhlottery.co.kr")) {
                                                isScanned = true

                                                // QR URL에서 모든 게임(최대 5게임)의 번호 파싱 후 각각 DB 저장
                                                parseAllLottoGamesFromUrl(qrResultUrl).forEach { (round, numbers) ->
                                                    purchaseViewModel.addPurchaseItem(round, numbers)
                                                }

                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrResultUrl))
                                                ctx.startActivity(intent)
                                            }
                                        }
                                    }

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
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
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "카메라 권한이 필요합니다",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                            ) {
                                Text(text = "권한 허용하기", color = Color.White)
                            }
                        }
                    }
                }

                Text(
                    text = "자동으로 당첨 결과가 스캔됩니다",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * 동행복권 QR 코드 URL에서 여러 게임(A~E)의 번호를 모두 파싱
 */
private fun parseAllLottoGamesFromUrl(url: String): List<Pair<Int, List<Int>>> {
    val results = mutableListOf<Pair<Int, List<Int>>>()
    try {
        val uri = Uri.parse(url)
        val vParam = uri.getQueryParameter("v") ?: return results
        
        // 동행복권 QR 코드는 게임별로 'q' 또는 기타 구분자로 나누어져 있거나 연속될 수 있음
        // 일반적인 형식: 회차번호 + m + 게임별 번호들 (구분자 q로 여러 게임 분리)
        val gameTokens = vParam.split("q")
        
        for (token in gameTokens) {
            val parts = token.split("m")
            if (parts.size >= 2) {
                val round = parts[0].toIntOrNull() ?: continue
                val numbersStr = parts[1]
                val numbers = mutableListOf<Int>()
                
                var i = 0
                while (i < numbersStr.length - 1) {
                    val numStr = numbersStr.substring(i, i + 2)
                    numStr.toIntOrNull()?.let { numbers.add(it) }
                    i += 2
                }

                if (numbers.size >= 6) {
                    results.add(Pair(round, numbers.take(6)))
                }
            } else {
                // 만약 m 페어가 하나만 있는 경우 전체 문자열 처리
                val partsMain = vParam.split("m")
                if (partsMain.size >= 2) {
                    val round = partsMain[0].toIntOrNull() ?: continue
                    val numbersStr = partsMain[1]
                    val numbers = mutableListOf<Int>()
                    var i = 0
                    while (i < numbersStr.length - 1) {
                        val numStr = numbersStr.substring(i, i + 2)
                        numStr.toIntOrNull()?.let { numbers.add(it) }
                        i += 2
                    }
                    if (numbers.size >= 6) {
                        results.add(Pair(round, numbers.take(6)))
                    }
                }
                break
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    // 중복 추가 방지 및 결과 반환
    return results.distinct()
}

private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onQrDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    when (barcode.valueType) {
                        Barcode.TYPE_URL, Barcode.TYPE_TEXT -> {
                            val rawValue = barcode.rawValue
                            if (rawValue != null) {
                                onQrDetected(rawValue)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
