import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

// Demonstrates: interfaces
// Any class that implements Savable must define toCSV()
interface Savable {
    String toCSV();
}

abstract class BasePokemon implements Savable {
    protected String name;
    protected int id;
    protected double height, weight;
    protected String types;
    protected int[] stats;

    // initializes all fields when object is created
    public BasePokemon(String name, int id, double height, double weight, String types, int[] stats) {
        this.name = name;
        this.id = id;
        this.height = height;
        this.weight = weight;
        this.types = types;
        this.stats = stats;
    }

    abstract String getSummary();

    // Converts this object to a CSV row string using Arrays.toString for the stats array
    @Override
    public String toCSV() {
        return id + "," + name + "," + height + "," + weight + "," + types + "," + Arrays.toString(stats);
    }
}

// adds spriteUrl field on top of what BasePokemon provides
class EnhancedPokemon extends BasePokemon {
    private String spriteUrl;

    public EnhancedPokemon(String name, int id, double height, double weight, String types, int[] stats, String spriteUrl) {
        super(name, id, height, weight, types, stats);
        this.spriteUrl = spriteUrl;
    }
    // overriding abstract method from BasePokemon
    @Override
    String getSummary() {
        return name.toUpperCase() + " #" + id + " | Types: " + types;
    }

    public String getSpriteUrl() { return spriteUrl; }
}
// main class, shows GUI, Event handling, API calls, ect. 
public class PokemonManager extends JFrame {
	
	// fixed-size collection of up to 100 Pokemon objects
    private EnhancedPokemon[] collection = new EnhancedPokemon[100];
    // tracks how many Pokemon are currently stored
    private int count = 0;
    //debug output to console
    private boolean debugMode = true;

    private JTextField searchField;
    private JTextArea infoArea;
    private JList<String> collectionList;
    private DefaultListModel<String> listModel;
    private ImagePanel spritePanel; // custom graphics panel

    private static final String BASE_API = "https://pokeapi.co/api/v2/pokemon/";
    private static final String[] STAT_NAMES = {"HP", "Attack", "Defense", "Sp. Atk", "Sp. Def", "Speed"};

    // builds and displays the GUI
    public PokemonManager() {
        setTitle("Pokémon Collection Manager - Final Project");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // buttons and text field in a top panel
        JPanel topPanel = new JPanel();
        searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search API");
        JButton randomBtn = new JButton("Random Pokémon");
        JButton sortBtn = new JButton("Sort Collection");
        JButton saveFileBtn = new JButton("Save to CSV");
        JButton loadFileBtn = new JButton("Load CSV");
        JButton saveDBBtn = new JButton("Save to DB");
        JButton loadDBBtn = new JButton("Load from DB");

        topPanel.add(new JLabel("Pokémon name/ID:"));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(randomBtn);
        topPanel.add(sortBtn);
        topPanel.add(saveFileBtn);
        topPanel.add(loadFileBtn);
        topPanel.add(saveDBBtn);
        topPanel.add(loadDBBtn);

        add(topPanel, BorderLayout.NORTH);

        // center panel split between text and sprite graphics
        JPanel center = new JPanel(new GridLayout(1, 2));
        infoArea = new JTextArea();
        infoArea.setEditable(false);
        spritePanel = new ImagePanel();
        center.add(new JScrollPane(infoArea));
        center.add(spritePanel);
        add(center, BorderLayout.CENTER);

        // scrollable list on the right showing the collection
        listModel = new DefaultListModel<>();
        collectionList = new JList<>(listModel);
        add(new JScrollPane(collectionList), BorderLayout.EAST);

        searchBtn.addActionListener(e -> fetchAndAdd(searchField.getText().trim().toLowerCase()));
        randomBtn.addActionListener(e -> {
            int randomId = (int)(Math.random() * 1025) + 1;
            fetchAndAdd(String.valueOf(randomId));
        });
        sortBtn.addActionListener(e -> bubbleSortCollection());
        saveFileBtn.addActionListener(e -> saveToCSV());
        loadFileBtn.addActionListener(e -> loadFromCSV());
        saveDBBtn.addActionListener(e -> saveToDatabase());
        loadDBBtn.addActionListener(e -> loadFromDatabase());

        setVisible(true);
    }

    private void fetchAndAdd(String query) {
        try {
            if (debugMode) System.out.println("DEBUG: Fetching " + query);

            // builds and sends an HTTP GET request to PokeAPI
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_API + query)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // check HTTP status before processing
            if (response.statusCode() != 200) {
                JOptionPane.showMessageDialog(this, "Pokémon not found!");
                return;
            }

            String json = response.body();

            // manually extract fields from JSON without external libraries
            int nameIndex = json.indexOf("\"base_experience\"");
            String name = extract(json.substring(nameIndex), "\"name\":\"", "\"");
            int id = parseIntSafe(extract(json, "\"id\":", ","));
            double height = parseDoubleSafe(extract(json, "\"height\":", ",")) / 10.0;
            double weight = parseDoubleSafe(extract(json, "\"weight\":", ",")) / 10.0;
            String types = extractTypes(json);
            String spriteUrl = extractSpriteUrl(json);
            int[] stats = extractStats(json);

            EnhancedPokemon p = new EnhancedPokemon(name, id, height, weight, types, stats, spriteUrl);

            if (count < collection.length) {
                collection[count++] = p;
                listModel.addElement(p.getSummary());
                // string concatenation to build multi-line display text
                infoArea.setText(
                    p.getSummary()
                    + "\nHeight: " + height + "m"
                    + "\nWeight: " + weight + "kg"
                    + "\n" + statsToText(stats)
                );
                spritePanel.loadImage(spriteUrl, stats); // graphics update
            }

        } catch (Exception ex) {
        	// catches any error and shows a dialog instead of crashing
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            if (debugMode) ex.printStackTrace();
        }
    }

    // finds the value between a start and end marker in a JSON string
    // returns empty string if either marker is missing
    private String extract(String json, String start, String end) {
        int s = json.indexOf(start);
        if (s == -1) return "";
        s += start.length();
        int e = json.indexOf(end, s);
        if (e == -1) return "";
        return json.substring(s, e).replace("\"", "").trim();
    }

    // loops through the JSON finding each type name and appends them
    private String extractTypes(String json) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while ((idx = json.indexOf("\"type\":{\"name\":\"", idx)) != -1) {
            idx += 16;
            int e = json.indexOf("\"", idx);
            sb.append(json.substring(idx, e)).append(" ");
            idx = e;
        }
        return sb.toString().trim();
    }

    private String extractSpriteUrl(String json) {
        int spritesIndex = json.indexOf("\"sprites\":");
        if (spritesIndex == -1) return "";
        String spritesJson = json.substring(spritesIndex);
        int idx = 0;
        while ((idx = spritesJson.indexOf("\"front_default\":", idx)) != -1) {
            idx += "\"front_default\":".length();
            while (idx < spritesJson.length() && spritesJson.charAt(idx) == ' ') idx++;
            if (spritesJson.startsWith("null", idx)) {
                idx += 4;
                continue;
            }
            if (spritesJson.charAt(idx) == '"') {
                idx++;
                int end = spritesJson.indexOf("\"", idx);
                if (end != -1) return spritesJson.substring(idx, end);
            }
        }
        return "";
    }

    // pulls all 6 base_stat values from JSON into an int array
    private int[] extractStats(String json) {
        int[] stats = new int[6]; // array declaration and initialization
        int idx = 0;
        for (int i = 0; i < stats.length; i++) {
            idx = json.indexOf("\"base_stat\":", idx);
            if (idx == -1) break;
            idx += "\"base_stat\":".length();
            int e = json.indexOf(",", idx);
            if (e == -1) break;
            stats[i] = parseIntSafe(json.substring(idx, e).trim());
            idx = e;
        }
        return stats;
    }

    // builds a readable multi-line string from the stats array
    private String statsToText(int[] stats) {
        StringBuilder sb = new StringBuilder("Stats:");
        for (int i = 0; i < STAT_NAMES.length && i < stats.length; i++) {
            sb.append("\n").append(STAT_NAMES[i]).append(": ").append(stats[i]);
        }
        return sb.toString();
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; } // exception handling
    }

    private double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return 0.0; } // exception handling
    }

    // requires collection to be sorted first, cuts search space in half each step
    private void binarySearchCollection(String name) {
        int low = 0, high = count - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (collection[mid].name.equalsIgnoreCase(name)) {
                JOptionPane.showMessageDialog(this, "Found: " + collection[mid].getSummary());
                return;
            }
            if (collection[mid].name.compareToIgnoreCase(name) < 0) low = mid + 1;
            else high = mid - 1;
        }
        JOptionPane.showMessageDialog(this, "Not found in collection");
    }

    // sorts collection alphabetically by Pokemon name
    private void bubbleSortCollection() {
        for (int i = 0; i < count - 1; i++) {
            for (int j = 0; j < count - i - 1; j++) {
                if (collection[j].name.compareTo(collection[j + 1].name) > 0) {
                	// temp variable swap pattern
                    EnhancedPokemon temp = collection[j];
                    collection[j] = collection[j + 1];
                    collection[j + 1] = temp;
                }
            }
        }
        refreshList(); // update the GUI after sorting 
    }

    // writes each Pokemon's toCSV() output as a line in the file
    private void refreshList() {
        listModel.clear();
        for (int i = 0; i < count; i++) {
            listModel.addElement(collection[i].getSummary());
        }
    }

    private void saveToCSV() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("pokemon_collection.csv"))) {
            for (int i = 0; i < count; i++) {
                pw.println(collection[i].toCSV());
            }
            JOptionPane.showMessageDialog(this, "Saved to pokemon_collection.csv");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "File error: " + e.getMessage());
        }
    }

    // reads each CSV line and reconstructs EnhancedPokemon objects
    private void loadFromCSV() {
        try (BufferedReader br = new BufferedReader(new FileReader("pokemon_collection.csv"))) {
            count = 0;
            String line;
            while ((line = br.readLine()) != null && count < collection.length) {
                if (line.trim().isEmpty()) continue;

                // Format: id,name,height,weight,types,[hp, atk, def, spatk, spdef, spd]
                // Split on first 5 commas only, leave stats blob intact
                String[] fields = line.split(",", 6);
                if (fields.length < 6) continue;

                int id = parseIntSafe(fields[0].trim());
                String name = fields[1].trim();
                double height = parseDoubleSafe(fields[2].trim());
                double weight = parseDoubleSafe(fields[3].trim());
                String types = fields[4].trim();

                // Parse "[hp, atk, def, spatk, spdef, spd]"
                String statsRaw = fields[5].replace("[", "").replace("]", "").trim();
                String[] statParts = statsRaw.split(",");
                int[] stats = new int[6];
                for (int i = 0; i < statParts.length && i < 6; i++) {
                    stats[i] = parseIntSafe(statParts[i].trim());
                }

                collection[count++] = new EnhancedPokemon(name, id, height, weight, types, stats, "");
            }
            refreshList();
            JOptionPane.showMessageDialog(this, "Loaded " + count + " Pokémon from CSV");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "File error: " + e.getMessage());
        }
    }

    private void saveToDatabase() {
        String url = "jdbc:sqlite:pokemon.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            // creates table if it doesn't already exist
            stmt.execute("CREATE TABLE IF NOT EXISTS pokemon (id INT, name VARCHAR(50), height DOUBLE, weight DOUBLE, types VARCHAR(100))");
            // clears old data before re-inserting
            stmt.execute("DELETE FROM pokemon");
            // inserts each Pokemon in the collection as a DB row
            for (int i = 0; i < count; i++) {
                String sql = "INSERT INTO pokemon VALUES (" + collection[i].id + ", '" + collection[i].name + "', "
                        + collection[i].height + ", " + collection[i].weight + ", '" + collection[i].types + "')";
                stmt.executeUpdate(sql);
            }
            JOptionPane.showMessageDialog(this, "Saved to Database!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
        }
    }

    private void loadFromDatabase() {
        String url = "jdbc:sqlite:pokemon.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            // retrieves all rows from the pokemon table
            ResultSet rs = stmt.executeQuery("SELECT * FROM pokemon");
            count = 0;
            while (rs.next() && count < collection.length) {
                collection[count++] = new EnhancedPokemon(
                    rs.getString("name"), rs.getInt("id"),
                    rs.getDouble("height"), rs.getDouble("weight"),
                    rs.getString("types"), new int[6], ""
                );
            }
            refreshList();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
        }
    }

    // displays the Pokemon sprite and draws all 6 stat bars
    class ImagePanel extends JPanel {
        private BufferedImage sprite;
        private int[] currentStats = new int[6];

        // loads a sprite image from URL and stores stats for the bar chart
        public void loadImage(String urlStr, int[] stats) {
            currentStats = new int[STAT_NAMES.length];
            if (stats != null) {
                System.arraycopy(stats, 0, currentStats, 0, Math.min(stats.length, currentStats.length));
            }
            sprite = null;
            try {
                if (urlStr != null && !urlStr.isEmpty()) {
                    sprite = ImageIO.read(new URL(urlStr));
                }
            } catch (Exception e) {
                if (debugMode) System.out.println("DEBUG: Could not load sprite");
            }
            repaint();
        }

        // draws the sprite image and six color-coded stat bars
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            if (sprite != null) {
                g2.drawImage(sprite, 50, 50, 200, 200, this);
            }

            int labelX = 10, barX = 95, y = 300;
            int barHeight = 18, maxBarWidth = 220, maxStat = 255;
            Color[] statColors = {
                new Color(220, 53, 69),   // HP
                new Color(255, 193, 7),   // Attack
                new Color(40, 167, 69),   // Defense
                new Color(111, 66, 193),  // Sp. Atk
                new Color(23, 162, 184),  // Sp. Def
                new Color(253, 126, 20)   // Speed
            };

            for (int i = 0; i < STAT_NAMES.length && i < currentStats.length; i++) {
                int statValue = currentStats[i];
                int barWidth = Math.min(maxBarWidth, (int)((statValue / (double) maxStat) * maxBarWidth));

                g2.setColor(Color.BLACK);
                g2.drawString(STAT_NAMES[i] + ": " + statValue, labelX, y + 14);
                g2.setColor(new Color(220, 220, 220));
                g2.fillRect(barX, y, maxBarWidth, barHeight);
                g2.setColor(statColors[i]);
                g2.fillRect(barX, y, barWidth, barHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(barX, y, maxBarWidth, barHeight);

                y += 28;
            }
        }
    }

    // ensures GUI is created
    public static void main(String[] args) {
        SwingUtilities.invokeLater(PokemonManager::new);
    }
}
