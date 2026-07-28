name: Build mod

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Configurar JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Configurar Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: 8.5

      - name: Build
        run: gradle build --stacktrace

      - name: Enviar .jar como artefato
        uses: actions/upload-artifact@v4
        with:
          name: micmod-jar
          path: build/libs/*.jar
