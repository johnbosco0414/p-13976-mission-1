package com

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppTest {
    @TempDir
    lateinit var tempDir: File // 각 테스트를 위한 임시 폴더

    // 테스트 중에 출력을 캡처하는 헬퍼 함수
    private fun captureOutput(block: () -> Unit): String {
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        block()
        System.setOut(originalOut)
        return outputStream.toString().trim()
    }

    // 캐리지 리턴(\r) 문자를 제거하여 줄바꿈 문자를 통일시키는 함수
    private fun String.withoutCr(): String {
        return this.replace("\r", "")
    }

    // --- 파일 기능이 필요 없는 기본 테스트들 ---
    @Test
    fun `종료`() {
        val commands = """
        종료
        """.trimIndent()
        val scanner = Scanner(ByteArrayInputStream(commands.toByteArray()))
        // dbDir을 넘기지 않으면 파일 기능은 동작하지 않음
        val output = captureOutput { App(scanner).run() }

        val expected = """
        == 명언 앱 ==
        명령) 명언 앱을 종료합니다.
        """.trimIndent()

        assertEquals(expected.withoutCr(), output.withoutCr())
    }

    @Test
    fun `알수없는_명령어`() {
        val commands = """
        아무거나
        종료
        """.trimIndent()
        val scanner = Scanner(ByteArrayInputStream(commands.toByteArray()))
        val output = captureOutput { App(scanner).run() }

        assertTrue(output.contains("알 수 없는 명령입니다."))
    }


    // --- 파일 영속성 기능 테스트 ---

    @Test
    fun `등록_시_파일_생성`() {
        val commands = """
        등록
        테스트 명언
        테스트 작가
        종료
        """.trimIndent()
        val scanner = Scanner(ByteArrayInputStream(commands.toByteArray()))

        // App을 임시 폴더 경로와 함께 실행
        App(scanner, dbDir = tempDir.absolutePath).run()

        val wiseSayingDir = File(tempDir, "wiseSaying")
        val lastIdFile = File(wiseSayingDir, "lastId.txt")
        val sayingFile = File(wiseSayingDir, "1.json")

        assertTrue(lastIdFile.exists(), "lastId.txt 파일이 생성되어야 합니다.")
        assertEquals("1", lastIdFile.readText(), "lastId.txt의 내용은 1이어야 합니다.")

        assertTrue(sayingFile.exists(), "1.json 파일이 생성되어야 합니다.")
        val savedSaying = Gson().fromJson(sayingFile.readText(), WiseSaying::class.java)
        assertEquals(1, savedSaying.id)
        assertEquals("테스트 명언", savedSaying.content)
        assertEquals("테스트 작가", savedSaying.author)
    }

    @Test
    fun `앱_시작_시_파일_로드`() {
        // 미리 테스트 파일들을 준비
        val wiseSayingDir = File(tempDir, "wiseSaying")
        wiseSayingDir.mkdirs()
        File(wiseSayingDir, "lastId.txt").writeText("1")
        val sayingJson = Gson().toJson(WiseSaying(1, "기존 명언", "기존 작가"))
        File(wiseSayingDir, "1.json").writeText(sayingJson)

        val commands = """
        목록
        종료
        """.trimIndent()
        val scanner = Scanner(ByteArrayInputStream(commands.toByteArray()))

        // 새 App 인스턴스가 기존 파일을 읽어들여야 함
        val output = captureOutput { App(scanner, dbDir = tempDir.absolutePath).run() }

        assertTrue(output.contains("1 / 기존 명언 / 기존 작가"), "앱 시작 시 로드된 명언이 목록에 표시되어야 합니다.")
    }

    @Test
    fun `삭제_시_파일_삭제`() {
        // 등록 먼저 실행 (별도의 App 인스턴스)
        val registerCommands = """
        등록
        삭제될 명언
        삭제될 작가
        종료
        """.trimIndent()
        val scanner1 = Scanner(ByteArrayInputStream(registerCommands.toByteArray()))
        App(scanner1, dbDir = tempDir.absolutePath).run()

        val sayingFile = File(tempDir, "wiseSaying/1.json")
        assertTrue(sayingFile.exists(), "삭제 전에는 파일이 존재해야 합니다.")

        // 삭제 실행 (별도의 App 인스턴스)
        val deleteCommands = """
        삭제?id=1
        종료
        """.trimIndent()
        val scanner2 = Scanner(ByteArrayInputStream(deleteCommands.toByteArray()))
        App(scanner2, dbDir = tempDir.absolutePath).run()

        assertFalse(sayingFile.exists(), "삭제 후에는 파일이 존재하지 않아야 합니다.")
    }

    @Test
    fun `수정_시_파일_수정`() {
        // 등록 먼저 실행
        val registerCommands = """
        등록
        옛 명언
        옛 작가
        종료
        """.trimIndent()
        val scanner1 = Scanner(ByteArrayInputStream(registerCommands.toByteArray()))
        App(scanner1, dbDir = tempDir.absolutePath).run()

        // 수정 실행
        val modifyCommands = """
        수정?id=1
        새 명언
        새 작가
        종료
        """.trimIndent()
        val scanner2 = Scanner(ByteArrayInputStream(modifyCommands.toByteArray()))
        App(scanner2, dbDir = tempDir.absolutePath).run()

        val sayingFile = File(tempDir, "wiseSaying/1.json")
        val updatedSaying = Gson().fromJson(sayingFile.readText(), WiseSaying::class.java)

        assertEquals("새 명언", updatedSaying.content, "수정된 내용이 파일에 반영되어야 합니다.")
        assertEquals("새 작가", updatedSaying.author, "수정된 작가가 파일에 반영되어야 합니다.")
    }
}