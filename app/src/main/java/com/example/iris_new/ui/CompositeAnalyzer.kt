package com.example.iris_new.ui

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class CompositeAnalyzer(
    private val analyzers: List<ImageAnalysis.Analyzer>
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        try {
            analyzers.forEach { analyzer ->
                analyzer.analyze(imageProxy)
            }
        } finally {
            // ✅ CLOSE ONCE, HERE
            imageProxy.close()
        }
    }
}
