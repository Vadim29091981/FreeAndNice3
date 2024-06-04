package fr.mossaab.security.controller;

import fr.mossaab.security.entities.PassportCategory;
import fr.mossaab.security.entities.PassportFileData;
import fr.mossaab.security.entities.PassportTitle;
import fr.mossaab.security.repository.PassportCategoryRepository;
import fr.mossaab.security.repository.PassportFileDataRepository;
import fr.mossaab.security.repository.PassportTitleRepository;
import fr.mossaab.security.service.impl.PassportService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/passport")
@RequiredArgsConstructor
public class PassportController {

    private final PassportCategoryRepository passportCategoryRepository;
    private final PassportTitleRepository passportTitleRepository;
    private final PassportFileDataRepository passportFileDataRepository;
    private final PassportService passportService;

    @GetMapping("/categories")
    public List<CategoryWithTitlesDTO> getAllCategoriesWithTitlesAndFiles() {
        List<CategoryWithTitlesDTO> categoryWithTitlesList = new ArrayList<>();

        List<PassportCategory> categories = passportCategoryRepository.findAll();
        for (PassportCategory category : categories) {
            List<PassportTitleWithFilesDTO> titleWithFilesList = new ArrayList<>();
            List<PassportTitle> titles = passportTitleRepository.findAllByCategory(category);
            for (PassportTitle title : titles) {
                List<PassportFileData> fileDataList = passportFileDataRepository.findByPassportTitle(title);
                List<String> filePaths = fileDataList.stream()
                        .map(fileData -> "http://31.129.102.70:8080/passport/image/" + fileData.getName()) // Добавление хоста к каждому имени файла
                        .collect(Collectors.toList()); // Собираем все имена файлов в список

                titleWithFilesList.add(new PassportTitleWithFilesDTO(title.getTitle(), filePaths));
            }
            categoryWithTitlesList.add(new CategoryWithTitlesDTO(category.getTitle(), titleWithFilesList));
        }

        return categoryWithTitlesList;
    }

    @GetMapping("/image/{fileName}")
    public ResponseEntity<?> getImage(@PathVariable String fileName) {
        try {
            String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

            // Получаем путь к файлу изображения
            Path pathToImage = Paths.get(getClass().getResource("/images/" + decodedFileName).toURI());

            // Читаем данные изображения из файла
            byte[] imageData;
            try (InputStream inputStream = new FileInputStream(pathToImage.toFile())) {
                imageData = inputStream.readAllBytes();
            }

            // Определяем MIME-тип изображения
            String mimeType = Files.probeContentType(pathToImage);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.valueOf(mimeType))
                    .body(imageData);

        } catch (URISyntaxException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при загрузке изображения: " + e.getMessage());
        }
    }

    @Setter
    @Getter
    // DTO class to hold category name and its titles
    static class CategoryWithTitlesDTO {
        private String categoryName;
        private List<PassportTitleWithFilesDTO> titles;

        public CategoryWithTitlesDTO(String categoryName, List<PassportTitleWithFilesDTO> titles) {
            this.categoryName = categoryName;
            this.titles = titles;
        }

        // getters and setters
    }
    @Setter
    @Getter
    // DTO class to hold title name and its files
    static class PassportTitleWithFilesDTO {
        private String titleName;
        private List<String> files;

        public PassportTitleWithFilesDTO(String titleName, List<String> files) {
            this.titleName = titleName;
            this.files = files;
        }

        // getters and setters
    }
}