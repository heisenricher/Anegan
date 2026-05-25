package com.aleph.core.conversion

import java.io.File

interface DocumentConverter {
    suspend fun convertToPdf(input: File): Result<File>
    suspend fun mergePdfs(inputs: List<File>, outputName: String): Result<File>
    suspend fun removeProtection(input: File, password: String): Result<File>
}

class NativeDocumentConverter : DocumentConverter {
    override suspend fun convertToPdf(input: File): Result<File> {
        // TODO: Implement PDFBox or Android PrintedPdfDocument logic
        return Result.success(File(input.parent, "converted.pdf"))
    }

    override suspend fun mergePdfs(inputs: List<File>, outputName: String): Result<File> {
        // TODO: Implement PDF merging logic
        return Result.success(File(inputs.first().parent, "$outputName.pdf"))
    }

    override suspend fun removeProtection(input: File, password: String): Result<File> {
        return Result.success(File(input.parent, "unlocked.pdf"))
    }
}
