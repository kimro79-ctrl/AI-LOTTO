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

    var scanCount by remember { mutableStateOf(0) }
    var lastScannedUrl by remember { mutableStateOf<String?>(null) }
    var lastProcessedUrl by remember { mutableStateOf<String?>(null) } // 중복 방지 전용 변수 분리
    var isCoolingDown by remember { mutableStateOf(false) }

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
                Surface(
                    color = Color(0xFFE0F2FE),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (scanCount > 0) "저장 완료된 게임 수: ${scanCount}개" else "로또 용지의 QR코드를 비춰주세요",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(240.dp)
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

                                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                        processImageProxy(scanner, imageProxy) { qrResultUrl ->

                                            // 동행복권 QR만 허용
                                            if (!qrResultUrl.contains("dhlottery.co.kr")) {
                                                return@processImageProxy
                                            }

                                            // 이미 처리 중이면 무시
                                            if (isCoolingDown) {
                                                return@processImageProxy
                                            }

                                            // 동일 QR 연속 재인식이면 무시
                                            if (lastProcessedUrl == qrResultUrl) {
                                                return@processImageProxy
                                            }

                                            isCoolingDown = true
                                            lastScannedUrl = qrResultUrl      // 버튼 유지용
                                            lastProcessedUrl = qrResultUrl    // 중복 방지용

                                            val games = parseAllLottoGamesFromUrl(qrResultUrl)

                                            if (games.isNotEmpty()) {
                                                games.forEach { (round, numbers) ->
                                                    purchaseViewModel.addPurchaseItem(
                                                        round = round,
                                                        numbers = numbers
                                                    )
                                                }
                                                scanCount += games.size
                                            }

                                            // 2초 후 쿨다운 및 중복 방지 키만 해제 (버튼은 계속 유지됨)
                                            android.os.Handler(ctx.mainLooper).postDelayed({
                                                isCoolingDown = false
                                                lastProcessedUrl = null
                                            }, 2000)
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
                    text = "QR 코드를 갖다 대면 용지에 있는\n모든 게임 번호들이 각각 올바르게 저장됩니다.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                if (lastScannedUrl != null) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lastScannedUrl))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "동행복권에서 공식 당첨 결과 보기",
                            color = Color(0xFFFFFFFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

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
    
    return results
        .map { it.first to it.second.sorted() }
        .distinct()
}

private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onQrDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
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
