import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

interface Savable {
    String toCSV();
}

abstract class BasePokemon implements Savable {
    protected String name;
    protected int id;
    protected double height, weight;
    protected String types;
    protected int[] stats;

    public BasePokemon(String name, int id, double height, double weight, String types, int[] stats) {
        this.name = name;
        this.id = id;
        this.height = height;
        this.weight = weight;
        this.types = types;
        this.stats = stats;
    }

    abstract String getSummary();

    @Override
    protected int statAt(int index) {
        return stats != null && index < stats.length ? stats[index] : 0;
    }

    protected String csvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public String toCSV() {
        return id + ","
                + csvValue(name) + ","
                + height + ","
                + weight + ","
                + csvValue(types) + ","
                + ","
                + statAt(0) + ","
                + statAt(1) + ","
                + statAt(2) + ","
                + statAt(3) + ","
                + statAt(4) + ","
                + statAt(5);
    }
}

class EnhancedPokemon extends BasePokemon {
    private String spriteUrl;

    public EnhancedPokemon(String name, int id, double height, double weight, String types, int[] stats, String spriteUrl) {
        super(name, id, height, weight, types, stats);
        this.spriteUrl = spriteUrl;
    }

    @Override
    String getSummary() {
        return name.toUpperCase() + " #" + id + " | Types: " + types;
    }

    public String getSpriteUrl() { return spriteUrl; }

    @Override
    public String toCSV() {
        return id + ","
                + csvValue(name) + ","
                + height + ","
                + weight + ","
                + csvValue(types) + ","
                + csvValue(spriteUrl) + ","
                + statAt(0) + ","
                + statAt(1) + ","
                + statAt(2) + ","
                + statAt(3) + ","
                + statAt(4) + ","
                + statAt(5);
    }
}

public class PokemonManager extends JFrame {

    private BasePokemon[] collection = new BasePokemon[100];
    private int count = 0;
    private boolean debugMode = true;

    private JTextField searchField;
    private JTextArea infoArea;
    private JList<String> collectionList;
    private DefaultListModel<String> listModel;
    private ImagePanel spritePanel;

    private static final String BASE_API = "https://pokeapi.co/api/v2/pokemon/";
    private static final String[] STAT_NAMES = {"HP", "Attack", "Defense", "Sp. Atk", "Sp. Def", "Speed"};
    private static final String DB_URL = "jdbc:sqlite:pokemon_db.db";

    public PokemonManager() {
        setTitle("Pokémon Collection Manager - Final Project");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

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

        JPanel center = new JPanel(new GridLayout(1, 2));
        infoArea = new JTextArea();
        infoArea.setEditable(false);
        spritePanel = new ImagePanel();
        center.add(new JScrollPane(infoArea));
        center.add(spritePanel);
        add(center, BorderLayout.CENTER);

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

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_API + query)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                JOptionPane.showMessageDialog(this, "Pokémon not found!");
                return;
            }

            String json = response.body();

            String name = extract(json, "\"name\":\"", "\"");
            int id = Integer.parseInt(extract(json, "\"id\":", ","));
            double height = Double.parseDouble(extract(json, "\"height\":", ",")) / 10.0;
            double weight = Double.parseDouble(extract(json, "\"weight\":", ",")) / 10.0;
            String types = extractTypes(json);
            String spriteUrl = extract(json, "\"front_default\":\"", "\"");

            int[] stats = extractStats(json);

            EnhancedPokemon p = new EnhancedPokemon(name, id, height, weight, types, stats, spriteUrl);

            if (count < collection.length) {
                collection[count++] = p;
                listModel.addElement(p.getSummary());
                infoArea.setText(
                        p.getSummary()
                                + "\nHeight: " + height + "m"
                                + "\nWeight: " + weight + "kg"
                                + "\n" + statsToText(stats)
                );
                spritePanel.loadPokemon(spriteUrl, stats);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            if (debugMode) ex.printStackTrace();
        }
    }

    private String extract(String json, String start, String end) {
        int s = json.indexOf(start) + start.length();
        int e = json.indexOf(end, s);
        return json.substring(s, e).replace("\"", "").trim();
    }

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

    private int[] extractStats(String json) {
        int[] stats = new int[6];
        int idx = 0;
        for (int i = 0; i < stats.length; i++) {
            idx = json.indexOf("\"base_stat\":", idx);
            if (idx == -1) {
                break;
            }
            idx += "\"base_stat\":".length();
            int e = json.indexOf(",", idx);
            stats[i] = Integer.parseInt(json.substring(idx, e).trim());
            idx = e;
        }
        return stats;
    }

    private String statsToText(int[] stats) {
        StringBuilder sb = new StringBuilder("Stats:");
        for (int i = 0; i < STAT_NAMES.length && i < stats.length; i++) {
            sb.append("\n").append(STAT_NAMES[i]).append(": ").append(stats[i]);
        }
        return sb.toString();
    }

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

    private void bubbleSortCollection() {
        for (int i = 0; i < count - 1; i++) {
            for (int j = 0; j < count - i - 1; j++) {
                if (collection[j].name.compareTo(collection[j + 1].name) > 0) {
                    BasePokemon temp = collection[j];
                    collection[j] = collection[j + 1];
                    collection[j + 1] = temp;
                }
            }
        }
        refreshList();
    }

    private void refreshList() {
        listModel.clear();
        for (int i = 0; i < count; i++) {
            listModel.addElement(collection[i].getSummary());
        }
    }

    private void saveToCSV() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("pokemon_collection.csv"))) {
            pw.println("id,name,height,weight,types,sprite_url,hp,attack,defense,special_attack,special_defense,speed");
            for (int i = 0; i < count; i++) {
                pw.println(collection[i].toCSV());
            }
            JOptionPane.showMessageDialog(this, "Saved to pokemon_collection.csv");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "File error: " + e.getMessage());
        }
    }

    private void loadFromCSV() {
        try (BufferedReader br = new BufferedReader(new FileReader("pokemon_collection.csv"))) {
            count = 0;
            String line;
            while ((line = br.readLine()) != null && count < collection.length) {
                if (line.trim().isEmpty() || line.startsWith("id,name,height")) {
                    continue;
                }

                String[] fields = parseCSVLine(line);
                if (fields.length < 12) {
                    if (debugMode) System.out.println("DEBUG: Skipping malformed CSV row: " + line);
                    continue;
                }

                int id = Integer.parseInt(fields[0]);
                String name = fields[1];
                double height = Double.parseDouble(fields[2]);
                double weight = Double.parseDouble(fields[3]);
                String types = fields[4];
                String spriteUrl = fields[5];
                int[] stats = {
                        Integer.parseInt(fields[6]),
                        Integer.parseInt(fields[7]),
                        Integer.parseInt(fields[8]),
                        Integer.parseInt(fields[9]),
                        Integer.parseInt(fields[10]),
                        Integer.parseInt(fields[11])
                };

                collection[count++] = new EnhancedPokemon(name, id, height, weight, types, stats, spriteUrl);
            }
            refreshList();
            showFirstLoadedPokemon();
            JOptionPane.showMessageDialog(this, "Loaded " + count + " Pokémon from pokemon_collection.csv");
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "File error: " + e.getMessage());
        }
    }

    private String[] parseCSVLine(String line) {
        java.util.List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private void showFirstLoadedPokemon() {
        if (count == 0) {
            infoArea.setText("");
            spritePanel.clearPokemon();
            return;
        }

        BasePokemon pokemon = collection[0];
        infoArea.setText(
                pokemon.getSummary()
                        + "\nHeight: " + pokemon.height + "m"
                        + "\nWeight: " + pokemon.weight + "kg"
                        + "\n" + statsToText(pokemon.stats)
        );

        if (pokemon instanceof EnhancedPokemon) {
            EnhancedPokemon enhanced = (EnhancedPokemon) pokemon;
            spritePanel.loadPokemon(enhanced.getSpriteUrl(), enhanced.stats);
        }
    }

    private void saveToDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS pokemon (id INT, name VARCHAR(50), height DOUBLE, weight DOUBLE, types VARCHAR(100))");

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
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM pokemon");
            count = 0;
            while (rs.next() && count < collection.length) {
                collection[count++] = new EnhancedPokemon(rs.getString("name"), rs.getInt("id"),
                        rs.getDouble("height"), rs.getDouble("weight"), rs.getString("types"), new int[6], "");
            }
            refreshList();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
        }
    }

    class ImagePanel extends JPanel {
        private BufferedImage sprite;
        private int[] currentStats = new int[6];

        public void clearPokemon() {
            sprite = null;
            currentStats = new int[6];
            repaint();
        }

        public void loadPokemon(String urlStr, int[] stats) {
            currentStats = new int[STAT_NAMES.length];
            if (stats != null) {
                System.arraycopy(stats, 0, currentStats, 0, Math.min(stats.length, currentStats.length));
            }
            sprite = null;
            try {
                sprite = ImageIO.read(new URL(urlStr));
            } catch (Exception e) {
                if (debugMode) System.out.println("DEBUG: Could not load sprite");
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            if (sprite != null) {
                g2.drawImage(sprite, 50, 50, 200, 200, this);
            }
            int labelX = 10;
            int barX = 95;
            int y = 300;
            int barHeight = 18;
            int maxBarWidth = 220;
            int maxStat = 255;

            for (int i = 0; i < STAT_NAMES.length && i < currentStats.length; i++) {
                int statValue = currentStats[i];
                int barWidth = Math.min(maxBarWidth, (int) ((statValue / (double) maxStat) * maxBarWidth));

                g2.setColor(Color.BLACK);
                g2.drawString(STAT_NAMES[i] + ": " + statValue, labelX, y + 14);
                g2.setColor(new Color(220, 220, 220));
                g2.fillRect(barX, y, maxBarWidth, barHeight);
                Color[] statColors = {
                        new Color(220, 53, 69),   // HP
                        new Color(255, 193, 7),   // Attack
                        new Color(40, 167, 69),   // Defense
                        new Color(111, 66, 193),  // Sp. Atk
                        new Color(23, 162, 184),  // Sp. Def
                        new Color(253, 126, 20)   // Speed
                };
                g2.setColor(statColors[i % statColors.length]);
                g2.fillRect(barX, y, barWidth, barHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(barX, y, maxBarWidth, barHeight);

                y += 28;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PokemonManager::new);
    }
}