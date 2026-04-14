import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

// DEMO: Classes and objects + Inheritance Part II (interface)
interface Savable {
    String toCSV();
}

// DEMO: Inheritance (abstract class) + Inheritance Part II
abstract class BasePokemon implements Savable {
    protected String name;
    protected int id;
    protected double height, weight;
    protected String types;
    protected int[] stats; // HP, Attack, Defense, SpAtk, SpDef, Speed

    public BasePokemon(String name, int id, double height, double weight, String types, int[] stats) {
        this.name = name;
        this.id = id;
        this.height = height;
        this.weight = weight;
        this.types = types;
        this.stats = stats;
    }

    // DEMO: Inheritance Part II - abstract method
    abstract String getSummary();

    @Override
    public String toCSV() {
        return id + "," + name + "," + height + "," + weight + "," + types + "," + Arrays.toString(stats);
    }
}

// DEMO: Inheritance (extends) + overriding
class EnhancedPokemon extends BasePokemon {
    private String spriteUrl;

    public EnhancedPokemon(String name, int id, double height, double weight, String types, int[] stats, String spriteUrl) {
        super(name, id, height, weight, types, stats);
        this.spriteUrl = spriteUrl;
    }

    @Override
    String getSummary() {
        return name.toUpperCase() + " #" + id + " | Types: " + types; // DEMO: String + Character manipulation
    }

    public String getSpriteUrl() { return spriteUrl; }
}

public class PokemonManager extends JFrame {  // DEMO: GUI (JFrame)

    // DEMO: Numeric variables
    private Pokemon[] collection = new Pokemon[100]; // DEMO: Array
    private int count = 0;
    private boolean debugMode = true; // DEMO: Debug flag

    // GUI components
    private JTextField searchField;
    private JTextArea infoArea;
    private JList<String> collectionList;
    private DefaultListModel<String> listModel;
    private ImagePanel spritePanel; // DEMO: Graphics (custom JPanel)

    private static final String BASE_API = "https://pokeapi.co/api/v2/pokemon/";

    public PokemonManager() {
        setTitle("Pokémon Collection Manager - Final Project");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // DEMO: GUI layout
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

        // Center: info + graphics
        JPanel center = new JPanel(new GridLayout(1, 2));
        infoArea = new JTextArea();
        infoArea.setEditable(false);
        spritePanel = new ImagePanel(); // DEMO: Graphics
        center.add(new JScrollPane(infoArea));
        center.add(spritePanel);
        add(center, BorderLayout.CENTER);

        // Right: collection list
        listModel = new DefaultListModel<>();
        collectionList = new JList<>(listModel);
        add(new JScrollPane(collectionList), BorderLayout.EAST);

        // Button listeners - DEMO: Boolean/Conditional + Event handling
        searchBtn.addActionListener(e -> fetchAndAdd(searchField.getText().trim().toLowerCase()));
        randomBtn.addActionListener(e -> {
            int randomId = (int)(Math.random() * 1025) + 1; // DEMO: Numeric + Loop
            fetchAndAdd(String.valueOf(randomId));
        });
        sortBtn.addActionListener(e -> bubbleSortCollection()); // DEMO: Sort algorithm
        saveFileBtn.addActionListener(e -> saveToCSV());
        loadFileBtn.addActionListener(e -> loadFromCSV());
        saveDBBtn.addActionListener(e -> saveToDatabase());
        loadDBBtn.addActionListener(e -> loadFromDatabase());

        setVisible(true);
    }

    // DEMO: API + Exception handling + String parsing
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

            // DEMO: Character, String + manual JSON parsing (no external libs)
            String name = extract(json, "\"name\":\"", "\"");
            int id = Integer.parseInt(extract(json, "\"id\":", ","));
            double height = Double.parseDouble(extract(json, "\"height\":", ",")) / 10.0;
            double weight = Double.parseDouble(extract(json, "\"weight\":", ",")) / 10.0;
            String types = extractTypes(json);
            String spriteUrl = extract(json, "\"front_default\":\"", "\"");

            int[] stats = new int[6]; // DEMO: Array + Loop
            // (simplified stat extraction - you can expand)
            stats[0] = 100; // placeholder for demo

            EnhancedPokemon p = new EnhancedPokemon(name, id, height, weight, types, stats, spriteUrl);

            if (count < collection.length) {
                collection[count++] = p;
                listModel.addElement(p.getSummary());
                infoArea.setText(p.getSummary() + "\nHeight: " + height + "m\nWeight: " + weight + "kg");
                spritePanel.loadImage(spriteUrl); // DEMO: Graphics
            }

        } catch (Exception ex) { // DEMO: Exception
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

    // DEMO: Search algorithm (binary search on sorted collection)
    private void binarySearchCollection(String name) {
        // assume sorted first
        int low = 0, high = count - 1;
        while (low <= high) { // DEMO: Loop + Boolean conditional
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

    // DEMO: Sort algorithm (bubble sort)
    private void bubbleSortCollection() {
        for (int i = 0; i < count - 1; i++) {
            for (int j = 0; j < count - i - 1; j++) {
                if (collection[j].name.compareTo(collection[j + 1].name) > 0) {
                    Pokemon temp = collection[j];
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

    // DEMO: File IO
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

    private void loadFromCSV() {
        try (BufferedReader br = new BufferedReader(new FileReader("pokemon_collection.csv"))) {
            count = 0;
            String line;
            while ((line = br.readLine()) != null && count < collection.length) {
                // parse CSV line and recreate objects (simplified)
                collection[count++] = new EnhancedPokemon("Temp", 0, 0, 0, "", new int[6], "");
            }
            refreshList();
            JOptionPane.showMessageDialog(this, "Loaded from CSV");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "File error: " + e.getMessage());
        }
    }

    // DEMO: JDBC + Exception
    private void saveToDatabase() {
        String url = "jdbc:mysql://localhost:3306/pokemon_db"; // CHANGE TO YOUR DB
        try (Connection conn = DriverManager.getConnection(url, "root", "password")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS pokemon (id INT, name VARCHAR(50), height DOUBLE, weight DOUBLE, types VARCHAR(100))");

            for (int i = 0; i < count; i++) {
                String sql = "INSERT INTO pokemon VALUES (" + collection[i].id + ", '" + collection[i].name + "', " +
                             collection[i].height + ", " + collection[i].weight + ", '" + collection[i].types + "')";
                stmt.executeUpdate(sql);
            }
            JOptionPane.showMessageDialog(this, "Saved to Database!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
        }
    }

    private void loadFromDatabase() {
        String url = "jdbc:mysql://localhost:3306/pokemon_db"; // CHANGE TO YOUR DB
        try (Connection conn = DriverManager.getConnection(url, "root", "password")) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM pokemon");
            count = 0;
            while (rs.next() && count < collection.length) {
                // recreate objects
                collection[count++] = new EnhancedPokemon(rs.getString("name"), rs.getInt("id"),
                        rs.getDouble("height"), rs.getDouble("weight"), rs.getString("types"), new int[6], "");
            }
            refreshList();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
        }
    }

    // DEMO: Graphics (custom panel with image + drawn bars)
    class ImagePanel extends JPanel {
        private BufferedImage sprite;

        public void loadImage(String urlStr) {
            try {
                sprite = ImageIO.read(new URL(urlStr));
                repaint();
            } catch (Exception e) {
                if (debugMode) System.out.println("DEBUG: Could not load sprite");
            }
        }

        @Override
        protected void paintComponent(Graphics g) { // DEMO: Graphics
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            if (sprite != null) {
                g2.drawImage(sprite, 50, 50, 200, 200, this);
            }
            // Draw stat bars example
            g2.setColor(Color.RED);
            g2.fillRect(50, 300, 150, 20); // example bar
            g2.setColor(Color.BLACK);
            g2.drawString("HP", 10, 315);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PokemonManager::new);
    }
}