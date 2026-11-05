package com.example.engine

import android.content.Context
import com.example.model.AnimationProject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/**
 * Manages local persistent storage of Kodanima 2D projects on device.
 */
object ProjectStorageManager {

    private const val PROJECTS_DIR = "kodanima_projects"

    private fun getProjectsDir(context: Context): File {
        val dir = File(context.filesDir, PROJECTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Saves or updates a project to internal storage as a .kodanima JSON file.
     */
    fun saveProject(context: Context, project: AnimationProject): Boolean {
        return try {
            val dir = getProjectsDir(context)
            val file = File(dir, "${project.id}.kodanima")
            val json = FileImportExportManager.projectToJson(project)
            FileOutputStream(file).use { it.write(json.toByteArray(Charsets.UTF_8)) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Loads all saved projects from the internal storage.
     * Seeds the demo project if the list is empty.
     */
    fun loadAllProjects(context: Context): List<AnimationProject> {
        val dir = getProjectsDir(context)
        val files = dir.listFiles { file -> file.extension == "kodanima" } ?: emptyArray()

        if (files.isEmpty()) {
            val demo = SampleProjects.createBouncingBallProject()
            saveProject(context, demo)
            return listOf(demo)
        }

        val projects = mutableListOf<AnimationProject>()
        for (file in files) {
            try {
                val json = FileInputStream(file).use { it.readBytes().toString(Charsets.UTF_8) }
                val proj = FileImportExportManager.projectFromJson(json)
                projects.add(proj)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Sort by most recently updated
        return projects.sortedByDescending { it.updatedAt }
    }

    /**
     * Deletes a project by ID from internal storage.
     */
    fun deleteProject(context: Context, projectId: String): Boolean {
        val dir = getProjectsDir(context)
        val file = File(dir, "$projectId.kodanima")
        return if (file.exists()) file.delete() else false
    }

    /**
     * Duplicates an existing project.
     */
    fun duplicateProject(context: Context, projectId: String): AnimationProject? {
        val dir = getProjectsDir(context)
        val file = File(dir, "$projectId.kodanima")
        if (!file.exists()) return null

        return try {
            val json = FileInputStream(file).use { it.readBytes().toString(Charsets.UTF_8) }
            val original = FileImportExportManager.projectFromJson(json)
            val newId = UUID.randomUUID().toString()
            val duplicate = original.copy(
                id = newId,
                name = "${original.name} (Cópia)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            saveProject(context, duplicate)
            duplicate
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
