package de.phlup.circuitchaos.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.phlup.circuitchaos.common.model.Board;
import de.phlup.circuitchaos.common.model.BoardInfo;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardLoader {

    private final ClientSettings boardProperties;
    private final ObjectMapper   objectMapper;

    private final ResourcePatternResolver resourceResolver =
            new PathMatchingResourcePatternResolver();

    private Image defaultImage;

    public List<BoardInfo> loadBoards(Image defaultImage) {
        this.defaultImage = defaultImage;
        return boardProperties.getBoardDirectories().stream()
                              .flatMap(this::findBoards)
                              .toList();
    }

    private Stream<BoardInfo> findBoards(String directory) {
        try {
            String pattern = directory + "/**/*.json";
            return Arrays.stream(resourceResolver.getResources(pattern))
                         .map(this::loadBoard)
                         .filter(Objects::nonNull);
        } catch (IOException e) {
            log.warn("Could not search for boards in {}", directory, e);
            return Stream.empty();
        }
    }

    private BoardInfo loadBoard(Resource jsonResource) {
        String filename = jsonResource.getFilename();
        if (filename == null || !filename.endsWith(".json")) {
            return null;
        }
        String name = filename.substring(0, filename.length() - ".json".length());
        try {
            Board board;
            try (InputStream inputStream = jsonResource.getInputStream()) {
                board = objectMapper.readValue(inputStream, Board.class);
            }
            Image image = findPreview(jsonResource);
            if (image == null) {
                image = defaultImage;
            }
            return new BoardInfo(name, image, board);
        } catch (IOException e) {
            log.warn("Could not load board {}", jsonResource, e);
            return null;
        }
    }

    private Image findPreview(Resource jsonResource) {
        try {
            String filename = jsonResource.getFilename();
            if (filename == null) {
                return null;
            }
            String pngFilename = replaceExtension(filename, ".png");
            if (jsonResource instanceof FileSystemResource fileResource) {
                Path jsonPath = fileResource.getFile().toPath();
                Path pngPath  = jsonPath.resolveSibling(pngFilename);
                if (!Files.exists(pngPath)) {
                    return null;
                }
                return ImageIO.read(pngPath.toFile());
            }
            if (jsonResource instanceof ClassPathResource classPathResource) {
                String jsonPath = classPathResource.getPath();
                String pngPath  = replaceFilename(jsonPath, pngFilename);
                return readPngImage(resourceResolver.getResource("classpath:" + pngPath));
            }
            return readPngImage(jsonResource.createRelative(pngFilename));
        } catch (IOException e) {
            log.warn("Could not load preview for {}", jsonResource, e);
            return null;
        }
    }

    private static BufferedImage readPngImage(Resource resource) throws IOException {
        if (!resource.exists()) {
            return null;
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return ImageIO.read(inputStream);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private String replaceExtension(String filename, String extension) {
        int index = filename.lastIndexOf('.');
        return index >= 0 ? filename.substring(0, index) + extension : filename + extension;
    }

    private String replaceFilename(String path, String filename) {
        int index = path.lastIndexOf('/');
        return index >= 0 ? path.substring(0, index + 1) + filename : filename;
    }

}
