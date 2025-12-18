package com.hugoof06.pokedex.ui;

import com.hugoof06.pokedex.cache.FilePokemonCache;
import com.hugoof06.pokedex.data.*;
import com.hugoof06.pokedex.favorites.FavoritesRepository;
import com.hugoof06.pokedex.favorites.FavoritesService;
import com.hugoof06.pokedex.model.Generation;
import com.hugoof06.pokedex.model.Pokemon;
import com.hugoof06.pokedex.model.Type;
import com.hugoof06.pokedex.service.PokedexService;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


import java.util.List;
import java.util.Optional;

public class MainView {

    private final BorderPane root = new BorderPane();

    // data/services
    private final FilePokemonCache cache = new FilePokemonCache();
    private PokemonRepository repo;
    private PokedexService service;

    private final FavoritesService favService =
            new FavoritesService(new FavoritesRepository());

    // UI state
    private Generation currentGen = Generation.GEN_1;
    private boolean favoritesOnly = false;
    private int pageSize = 20;
    private int page = 1;

    // UI components
    private final ComboBox<String> sourceBox = new ComboBox<>();
    private final ComboBox<Generation> genBox = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final Button searchBtn = new Button("Search");
    private final Button listBtn = new Button("List");
    private final Button prevBtn = new Button("<");
    private final Button nextBtn = new Button(">");
    private final Label pageLabel = new Label();
    private final ListView<String> listView = new ListView<>();
    private final VBox detailBox = new VBox(8);

    // current selection
    private Pokemon selectedPokemon;

    public MainView() {
        // default repo: API (puedes cambiar con UI)
        setSource("api");

        buildTopBar();
        buildCenter();
        buildBottomBar();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildTopBar() {
        sourceBox.setItems(FXCollections.observableArrayList("api", "json"));
        sourceBox.setValue("api");

        genBox.setItems(FXCollections.observableArrayList(Generation.values()));
        genBox.setValue(currentGen);

        searchField.setPromptText("Search name (e.g. char)");
        searchField.setPrefColumnCount(18);

        listBtn.setOnAction(e -> {
            page = 1;
            refreshList();
        });

        searchBtn.setOnAction(e -> {
            page = 1;
            refreshSearch();
        });

        genBox.setOnAction(e -> {
            currentGen = genBox.getValue();
            page = 1;
            refreshList();
        });

        sourceBox.setOnAction(e -> setSource(sourceBox.getValue()));

        CheckBox favOnlyCheck = new CheckBox("Show only favorites");
        favOnlyCheck.selectedProperty().addListener((obs, old, now) -> {
            favoritesOnly = now;
            page = 1;
            searchField.setDisable(now);
            searchBtn.setDisable(now);
            listBtn.setDisable(now);
            updatePagingUI();
            refreshListOrSearchDepending();
        });

        HBox top = new HBox(10,
                favOnlyCheck,
                new Label("Source:"), sourceBox,
                new Label("Gen:"), genBox,
                searchField, searchBtn, listBtn
        );
        top.setPadding(new Insets(10));
        root.setTop(top);
    }

    private void buildCenter() {
        listView.setPrefWidth(340);

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            if (now == null) return;

            // item format: "001 Bulbasaur" or "name"
            String token = now.split("\\s+")[0];
            String idOrName = token.matches("\\d{1,3}") ? String.valueOf(Integer.parseInt(token)) : now;

            Optional<Pokemon> p = service.show(idOrName);
            p.ifPresent(pk -> {
                selectedPokemon = pk;
                renderDetail(pk);
            });
        });

        detailBox.setPadding(new Insets(12));
        detailBox.getChildren().add(new Text("Select a Pokémon"));

        SplitPane split = new SplitPane();
        split.getItems().addAll(listView, new ScrollPane(detailBox));
        split.setDividerPositions(0.35);

        root.setCenter(split);
    }

    private void buildBottomBar() {
        prevBtn.setOnAction(e -> {
            if (page > 1) {
                page--;
                refreshListOrSearchDepending();
            }
        });

        nextBtn.setOnAction(e -> {
            page++;
            refreshListOrSearchDepending();
        });

        updatePageLabel();

        HBox bottom = new HBox(10, prevBtn, pageLabel, nextBtn,
                new Label("Cache:"), new Label(cache.location()));
        bottom.setPadding(new Insets(10));
        root.setBottom(bottom);
    }

    private void refreshListOrSearchDepending() {
        if (favoritesOnly) {
            refreshFavorites();
            return;
        }

        String q = searchField.getText().trim();
        if (q.isEmpty()) refreshList();
        else refreshSearch();
    }

    private void refreshList() {
        updatePageLabel();
        List<Pokemon> results = service.list(currentGen, page, pageSize);

        listView.setItems(FXCollections.observableArrayList(
                results.stream()
                        .map(p -> String.format("%03d %s", p.getId(), p.getName()))
                        .toList()
        ));

        if (!results.isEmpty()) {
            listView.getSelectionModel().select(0);
        } else {
            detailBox.getChildren().setAll(new Text("No results"));
        }
    }

    private void refreshSearch() {
        updatePageLabel();
        String q = searchField.getText().trim();
        if (q.isEmpty()) {
            refreshList();
            return;
        }

        // API-friendly: returns names only
        List<String> names = service.searchNames(currentGen, q, page, pageSize);

        listView.setItems(FXCollections.observableArrayList(
                names.stream().map(n -> n).toList()
        ));

        if (!names.isEmpty()) {
            listView.getSelectionModel().select(0);
        } else {
            detailBox.getChildren().setAll(new Text("No results"));
        }
    }

    private void renderDetail(Pokemon p) {
        Button favBtn = new Button(isFav(p.getId()) ? "★ Unfavorite" : "☆ Favorite");
        favBtn.setOnAction(e -> {
            if (isFav(p.getId())) {
                favService.remove(p.getId());
            } else {
                try {
                    favService.add(p.getId());
                } catch (IllegalStateException ex) {
                    alert(ex.getMessage());
                }
            }
            renderDetail(p);
        });

        ImageView sprite = new ImageView();
        sprite.setFitWidth(160);
        sprite.setFitHeight(160);
        sprite.setPreserveRatio(true);

        if (p.getSpriteUrl() != null && !p.getSpriteUrl().isBlank()) {
            sprite.setImage(new Image(p.getSpriteUrl(), true)); // carga en background
        }

        detailBox.getChildren().setAll(
                sprite,
                new Text("#" + String.format("%03d", p.getId()) + " " + p.getName()),
                new Text("Gen: " + p.getGeneration()),
                new Text("Types: " + p.getTypes()),
                new Text("Stats: " + p.getStats()),
                favBtn
        );
    }

    private boolean isFav(int id) {
        return favService.listIds().contains(id);
    }

    private void updatePageLabel() {
        updatePagingUI();
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.showAndWait();
    }

    private void setSource(String source) {
        PokemonRepository base = source.equals("json")
                ? new JsonPokemonRepository()
                : new PokeApiPokemonRepository();

        this.repo = new CachedPokemonRepository(base, cache);
        this.service = new PokedexService(repo);

        // reset de estado UI
        this.page = 1;
        this.selectedPokemon = null;

        refreshListOrSearchDepending();
    }

    private void updatePagingUI() {
        boolean pagingEnabled = !favoritesOnly;
        prevBtn.setDisable(!pagingEnabled);
        nextBtn.setDisable(!pagingEnabled);
        pageLabel.setText(favoritesOnly ? "Favorites" : ("Page " + page));
    }

    private void refreshFavorites() {
        updatePagingUI();

        var ids = favService.listIds();

        record LoadedItem(int id, Pokemon p) {}
        var loaded = new java.util.ArrayList<LoadedItem>();
        var notLoaded = new java.util.ArrayList<Integer>();

        for (int id : ids) {
            var opt = service.show(String.valueOf(id));
            if (opt.isPresent()) loaded.add(new LoadedItem(id, opt.get()));
            else notLoaded.add(id);
        }

        loaded.sort(java.util.Comparator
                .comparing((LoadedItem it) -> it.p.getGeneration())
                .thenComparingInt(it -> it.p.getId()));

        notLoaded.sort(Integer::compareTo);

        var lines = new java.util.ArrayList<String>();

        for (var it : loaded) {
            Pokemon p = it.p;
            lines.add(String.format("%03d %s [%s] %s",
                    p.getId(),
                    p.getName(),
                    p.getGeneration(),
                    p.getTypes()
            ));
        }
        for (int id : notLoaded) {
            lines.add(String.format("%03d (NOT LOADED)", id));
        }

        listView.setItems(FXCollections.observableArrayList(lines));

        if (!lines.isEmpty()) {
            listView.getSelectionModel().select(0);
        } else {
            detailBox.getChildren().setAll(new Text("No favorites yet"));
        }
    }
}
