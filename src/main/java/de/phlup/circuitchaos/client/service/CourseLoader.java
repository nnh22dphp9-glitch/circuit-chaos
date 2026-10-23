package de.phlup.circuitchaos.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.phlup.circuitchaos.common.model.Course;
import de.phlup.circuitchaos.common.model.CourseInfo;
import de.phlup.circuitchaos.common.settings.ClientSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.swing.JOptionPane;
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

    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public List<CourseInfo> loadCourses() {
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
            return new CourseInfo(name, course);
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
            if (course.getCheckpoints().size() < 2) {
                JOptionPane.showMessageDialog(null, "Successfully saved the course,\nbut it contains to finishing point, so it cannot be played.");
            } else {
                JOptionPane.showMessageDialog(null, "Successfully saved the course.");
            }
        } catch (IOException e) {
            log.warn("Could not save course {}", path, e);
            JOptionPane.showMessageDialog(null, "Could not save course to "
                    + path + ". See logs for details.");
        }
    }

}
