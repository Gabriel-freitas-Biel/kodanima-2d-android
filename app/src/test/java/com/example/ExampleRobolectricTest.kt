package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.FileImportExportManager
import com.example.engine.ProjectStorageManager
import com.example.engine.SampleProjects
import com.example.model.AppUiMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kodanima 2D", appName)
  }

  @Test
  fun `sample project serialization test`() {
    val demoProject = SampleProjects.createBouncingBallProject()
    assertTrue(demoProject.frames.size >= 8)

    val jsonString = FileImportExportManager.projectToJson(demoProject)
    assertNotNull(jsonString)
    assertTrue(jsonString.contains("Bouncing Ball"))

    val parsedProject = FileImportExportManager.projectFromJson(jsonString)
    assertEquals(demoProject.name, parsedProject.name)
    assertEquals(demoProject.frames.size, parsedProject.frames.size)
  }

  @Test
  fun `project storage manager save and load test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val demoProject = SampleProjects.createBouncingBallProject()
    val saved = ProjectStorageManager.saveProject(context, demoProject)
    assertTrue(saved)

    val allProjects = ProjectStorageManager.loadAllProjects(context)
    assertTrue(allProjects.isNotEmpty())

    val duplicated = ProjectStorageManager.duplicateProject(context, demoProject.id)
    assertNotNull(duplicated)
    assertTrue(duplicated!!.name.contains("Cópia"))
  }

  @Test
  fun `ui mode enum test`() {
    assertEquals("Aparência Mobile", AppUiMode.MOBILE.labelPt)
    assertEquals("Aparência PC (Desktop)", AppUiMode.PC.labelPt)
  }
}
