// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assert
import org.junit.Test

class AtoxArchitectureTest {

    @Test
    fun `presentation layer atox should not directly access database or JNI runtime`() {
        Konsist
            .scopeFromProject()
            .assertArchitecture {
                val ui = Layer("UI", "ltd.evilcorp.atox.ui..")
                val db = Layer("Database", "ltd.evilcorp.core.db..")
                val runtime = Layer("JniRuntime", "ltd.evilcorp.core.tox.runtime..")

                db.toString()
                runtime.toString()
                ui.dependsOnNothing()
            }
    }

    @Test
    fun `presentation layer atox should not declare any UseCase classes`() {
        Konsist
            .scopeFromPackage("ltd.evilcorp.atox..")
            .classes()
            .assert { !it.name.endsWith("UseCase") }
    }
}
