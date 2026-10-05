package de.phlup.circuitchaos.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.CourseInfo;
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
import javax.swing.JOptionPane;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseLoader {

    private final ClientSettings courseProperties;
    private final ObjectMapper   objectMapper;

    private final ResourcePatternResolver resourceResolver =
            new PathMatchingResourcePatternResolver();

    private Image defaultImage;

    public List<CourseInfo> loadCourses(Image defaultImage) {
        this.defaultImage = defaultImage;
        return courseProperties.getCourseDirectories().stream()
                               .flatMap(this::findCourses)
                               .toList();
    }

    private Stream<CourseInfo> findCourses(String directory) {
        try {
            String pattern = directory + "/**/*.json";
            return Arrays.stream(resourceResolver.getResources(pattern))
                         .map(this::loadCourse)
                         .filter(Objects::nonNull);
        } catch (IOException e) {
            log.warn("Could not search for courses in {}", directory, e);
            return Stream.empty();
        }
    }

    private CourseInfo loadCourse(Resource jsonResource) {
        String filename = jsonResource.getFilename();
        if (filename == null || !filename.endsWith(".json")) {
            return null;
        }
        String name = filename.substring(0, filename.length() - ".json".length());
        try {
            Course course;
            try (InputStream inputStream = jsonResource.getInputStream()) {
                course = objectMapper.readValue(inputStream, Course.class);
            }
            Image image = findPreview(jsonResource);
            if (image == null) {
                image = defaultImage;
            }
            return new CourseInfo(name, image, course);
        } catch (IOException e) {
            log.warn("Could not load course {}", jsonResource, e);
            return null;
        }
    }

    public Course loadCourse(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return objectMapper.readValue(inputStream, Course.class);
        } catch (IOException e) {
            log.warn("Could not load course {}", path, e);
            JOptionPane.showMessageDialog(null, "Could not load course from "
                    + path + ". See logs for details.");
            return null;
        }
    }

    public void saveCourse(Course course, Path path) {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(outputStream, course);
            JOptionPane.showMessageDialog(null, "Successfully saved the course.");
        } catch (IOException e) {
            log.warn("Could not save course {}", path, e);
            JOptionPane.showMessageDialog(null, "Could not save course to "
                    + path + ". See logs for details.");
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
