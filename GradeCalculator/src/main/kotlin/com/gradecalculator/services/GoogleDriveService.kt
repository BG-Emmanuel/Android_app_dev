package com.gradecalculator.services

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// ─────────────────────────────────────────────────────────────────────────────
// Google Drive Service
// ─────────────────────────────────────────────────────────────────────────────

class GoogleDriveService {

    companion object {
        private const val APPLICATION_NAME = "GradeCalculator"
        private val JSON_FACTORY           = GsonFactory.getDefaultInstance()
        private val SCOPES                 = listOf(DriveScopes.DRIVE_FILE)
    }

    private var driveService: Drive? = null

    fun initialize(credentialsFile: File) {
        val credentials = GoogleCredentials
            .fromStream(credentialsFile.inputStream())
            .createScoped(SCOPES)

        driveService = Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JSON_FACTORY,
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    /** Upload a local file to Google Drive */
    suspend fun uploadFile(localFile: File, mimeType: String, parentFolderId: String? = null): String =
        withContext(Dispatchers.IO) {
            val service = driveService
                ?: throw IllegalStateException("Google Drive not initialized")

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = localFile.name
                if (parentFolderId != null) parents = listOf(parentFolderId)
            }

            val mediaContent = com.google.api.client.http.FileContent(mimeType, localFile)

            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute()

            uploadedFile.webViewLink ?: uploadedFile.id
        }

    /** Download a file from Google Drive by ID */
    suspend fun downloadFile(fileId: String, destination: File) = withContext(Dispatchers.IO) {
        val service = driveService ?: throw IllegalStateException("Google Drive not initialized")
        val outputStream = FileOutputStream(destination)
        service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        outputStream.close()
    }

    /** List files in a specific folder */
    suspend fun listFiles(folderId: String? = null): List<DriveFile> = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext emptyList()
        val query = if (folderId != null) "'$folderId' in parents and trashed=false"
                    else "trashed=false"

        val result: FileList = service.files().list()
            .setQ(query)
            .setFields("files(id, name, mimeType, size, modifiedTime)")
            .execute()

        result.files?.map { f ->
            DriveFile(
                id           = f.id,
                name         = f.name,
                mimeType     = f.mimeType ?: "",
                size         = f.getSize() ?: 0L,
                modifiedTime = f.modifiedTime?.toString() ?: ""
            )
        } ?: emptyList()
    }

    val isInitialized: Boolean get() = driveService != null

    data class DriveFile(
        val id: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val modifiedTime: String
    )
}
