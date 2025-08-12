package com

import java.util.*

fun main() {
    val scanner = Scanner(System.`in`)
    // 실제 앱을 실행할 때는 "db" 폴더를 경로로 지정해줍니다.
    App(scanner, dbDir = "db").run()
}