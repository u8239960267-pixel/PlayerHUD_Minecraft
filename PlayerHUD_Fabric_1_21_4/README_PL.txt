PLAYER HUD — Fabric 1.21.4

To jest wariant projektu dla Minecrafta Java 1.21.4. Nie instaluj obu wersji moda jednocześnie.

Funkcje prototypu:
- Menu ustawień pod prawym Shiftem; skrót zmienisz w Opcje -> Sterowanie -> Player HUD.
- Włączanie/wyłączanie panelu, skina, życia/absorpcji i punktów zbroi.
- Celownik wskazuje przeciwnika; jeśli nie celujesz w gracza, wybierany jest najbliższy gracz do 8 bloków.
- Serduszka układane są po 10 na rząd.
- Pozycję panelu można przeciągnąć myszą, gdy otwarte jest menu.

OGRANICZENIA:
- W tej wersji widoczna jest twarz skina, nie pełny model 3D z renderowaną zbroją.
- Ustawienia nie są jeszcze zapisywane po restarcie.
- To projekt źródłowy; nie dostarczono skompilowanego pliku JAR. Kompilacja nie została tu zweryfikowana, ponieważ środowisko nie ma dostępu do pobierania zależności Gradle.

Budowanie:
1. Zainstaluj JDK 21 i Gradle kompatybilny z wersją Loom w projekcie.
2. Otwórz folder tego wariantu w IntelliJ IDEA jako projekt Gradle.
3. Uruchom zadanie Gradle `build`.
4. JAR pojawi się w `build/libs/`.
5. Zainstaluj Fabric Loader dla Minecrafta 1.21.4 i pasującą wersję Fabric API, a JAR umieść w `.minecraft/mods`.

To mod kliencki — nie trzeba instalować go na serwerze.
