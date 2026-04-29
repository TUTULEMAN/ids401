# IDS401 — Pokémon Manager

Simple Java Swing demo project for managing a small Pokémon collection. It demonstrates core Java concepts including object-oriented programming, GUI development, API integration, file I/O, and classic sorting/searching algorithms.

Contents:
- `pokemonmanager.java` — main source file

## Features
- **Search by name or ID** - fetch live Pokémon data from the [PokéAPI](https://pokeapi.co/)
- **Random Pokémon** - generates a random ID (1-1025) and fetched that Pokémon
- **Sprite display** — renders the official Pokémon front sprite using Java2D graphics
- **Stat bar visualization** — draws HP and stat bars directly on a custom graphics panel
- **Sort collection** — sorts your saved Pokémon alphabetically using bubble sort
- **Binary search** — search your sorted collection efficiently by name
- **Save/Load CSV** — persist your collection to a local `pokemon_collection.csv` file
- **Save/Load Database** — store and retrieve your collection from a MySQL database via JDBC

## Screenshots

**Opening screen:**
![Opening screen](opening_screen.png)
*The main window on launch — enter a Pokémon name or ID to search, or hit Random Pokémon.*

**Pokémon search result:**
![Search result](search_result.png)
*Search result for Pikachu (#25) showing its sprite, types, ability, height, weight, and all six stat bars.*


**Sort Collection view:**
![Sort collection](sort_collection.png)
*The Sort Collection view lists all saved Pokémon sorted alphabetically by name.*



GitHub Pages:
- A lightweight site is published from the `gh-pages` branch: https://TUTULEMAN.github.io/ids401/

How to run locally:
1. Compile:

```powershell
javac pokemonmanager.java
```

2. Run:

```powershell
java PokemonManager
```
## Database

This project uses SQLite, which stores the entire database as a single local file — no server setup required.

**Location:**
The database file `pokemon.db` is created automatically in whichever directory you run the program from. For example, if you compile and run from your project folder, it will appear there as:

/your/project/folder/pokemon.db

**How to view it in the terminal (Mac):**
**How to view it in the terminal (Mac):**
1. Open Terminal and navigate to the project folder:
```bash
cd /path/to/eclipse-workspace/PokemonManager
```
2. Open the database with the SQLite CLI:
```bash
sqlite3 pokemon.db
```
3. View the table contents:
```sql
SELECT * FROM pokemon;
```
4. Exit when done:
```bash
.quit
```

**What it contains:**
The `pokemon` table has one row per saved Pokémon with these columns:

| Column | Type | Example |
|--------|------|---------|
| id | INT | 25 |
| name | VARCHAR(50) | pikachu |
| height | DOUBLE | 0.4 |
| weight | DOUBLE | 6.0 |
| types | VARCHAR(100) | electric |


Notes:
- This project is a course demo; database credentials and API usage are placeholders.
- Files added for Pages live in the repository and are safe for public browsing.

Updates (4/28/26):
- Made the health bar dynamic so it scales based on each Pokemon's real stats.
- Added more dynamic stat bars at the bottom (Attack, Defense, Sp. Atk, Sp. Def, and Speed).
- Fixed the Excel/CSV export-import behavior so it is functional and no longer a placeholder.
