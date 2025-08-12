package com

import com.google.gson.Gson
import java.io.File
import java.util.*

class App(private val scanner: Scanner, private val dbDir: String? = null) {
    private val wiseSayings = mutableListOf<WiseSaying>()
    private var lastId = 0
    private val gson = Gson() // Json 변환을 위한 Gson 객체

    // 앱 실행시 데이터 로딩 로직
    init {
        if (dbDir != null) {
            val wiseSayingDir = File(dbDir, "wiseSaying")

            if (!wiseSayingDir.exists()) {
                wiseSayingDir.mkdirs()
            }

            val lastIdFile = File(wiseSayingDir, "lastId.txt")

            if (lastIdFile.exists()) {
                lastId = lastIdFile.readText().toIntOrNull() ?: 0
            } else {
                lastIdFile.createNewFile()
                lastIdFile.writeText("0")
            }

            val files = wiseSayingDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()

            for (file in files) {
                try {
                    val wiseSaying = gson.fromJson(file.readText(), WiseSaying::class.java)
                    wiseSayings.add(wiseSaying)
                } catch (e: Exception) {
                    println("파일 ${file.name}을 읽는 중 오류 발생: ${e.message}")
                }
            }
        }
    }

    fun run() {
        println("== 명언 앱 ==")

        while (true) {
            print("명령) ")

            val cmd = scanner.nextLine().trim()

            if (cmd.isEmpty()) {
                println("명령을 입력해주세요.")
                continue
            }

            if (cmd.startsWith("삭제?id=")) {
                try {
                    val removeId = cmd.split("=")[1].toInt()

                    val targetSaying = wiseSayings.find { it.id == removeId }

                    if (targetSaying == null) {
                        println("존재하지 않는 명언 번호입니다.")

                    } else {
                        wiseSayings.remove(targetSaying)

                        deleteWiseSayingFile(targetSaying.id)

                        println("${targetSaying.id}번 명언이 삭제되었습니다.")
                    }

                } catch (e: NumberFormatException) {
                    println("ID는 숫자로 입력해야 합니다.")
                }

                continue

            } else if (cmd.startsWith("수정?id=")) {
                try {
                    val updateId = cmd.split("=")[1].toInt()

                    var targetSaying = wiseSayings.find { it.id == updateId }

                    if (targetSaying == null) {
                        println("존재하지 않는 명언 번호입니다.")

                    } else {
                        println("명언(기존) : ${targetSaying.content}")
                        print("명언 : ")
                        val newContent = scanner.nextLine().trim()

                        println("작가(기존) : ${targetSaying.author}")
                        print("작가 : ")
                        val newAuthor = scanner.nextLine().trim()

                        val targetIndex = wiseSayings.indexOf(targetSaying)
                        val updatedSaying = targetSaying.copy(content = newContent, author = newAuthor)
                        wiseSayings[targetIndex] = updatedSaying

                        saveWiseSayingToFile(updatedSaying)

                        println("${targetSaying.id}번 명언이 수정되었습니다.")
                    }
                } catch (e: NumberFormatException) {
                    println("ID는 숫자로 입력해야 합니다.")
                }

                continue

            } else {

                when (cmd) {
                    "종료" -> {
                        println("명언 앱을 종료합니다.")
                        return
                    }

                    "등록" -> {
                        print("명언 : ")
                        val content = scanner.nextLine().trim()

                        print("작가 : ")
                        val author = scanner.nextLine().trim()

                        lastId++

                        val wiseSaying = WiseSaying(lastId, content, author)
                        wiseSayings.add(wiseSaying)

                        saveWiseSayingToFile(wiseSaying)
                        saveLastIdToFile()

                        println("${lastId}번 명언이 등록되었습니다.")
                    }

                    "목록" -> {
                        if (wiseSayings.isEmpty()) {
                            println("등록된 명언이 없습니다.")
                        } else {
                            println("== 명언 목록 ==")
                            println("번호 / 명언 / 작가")
                            println("-------------------")

                            for (wiseSaying in wiseSayings) {
                                println("${wiseSaying.id} / ${wiseSaying.content} / ${wiseSaying.author}")
                            }
                        }
                    }

                    else -> {
                        println("알 수 없는 명령입니다.")
                        println("종료하려면 '종료'라고 입력하세요.")
                    }
                }
            }
        }
    }

    private fun saveWiseSayingToFile(wiseSaying: WiseSaying) {
        if (dbDir == null) {
            return
        }

        val wiseSayingDir = File(dbDir, "wiseSaying")
        val file = File(wiseSayingDir, "${wiseSaying.id}.json")

        file.writeText(gson.toJson(wiseSaying))
    }

    private fun saveLastIdToFile() {
        if (dbDir == null) {
            return
        }

        val wiseSayingDir = File(dbDir, "wiseSaying")
        val lastIdFile = File(wiseSayingDir, "lastId.txt")

        lastIdFile.writeText(lastId.toString())
    }

    private fun deleteWiseSayingFile(id: Int) {
        if (dbDir == null) {
            return
        }

        val wiseSayingDir = File(dbDir, "wiseSaying")
        val file = File(wiseSayingDir, "$id.json")

        if (file.exists()) {
            file.delete()
        }
    }
}