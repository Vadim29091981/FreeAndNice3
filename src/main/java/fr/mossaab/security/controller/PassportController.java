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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
                // Using a loop to add the host to each file name
                List<String> filePaths = new ArrayList<>();
                for (PassportFileData fileData : fileDataList) {
                    String filePath = "http://31.129.102.70:8080/passport/image/" + fileData.getName();
                    filePaths.add(filePath);
                }
                titleWithFilesList.add(new PassportTitleWithFilesDTO(title.getTitle(),title.getRuTitle(), filePaths));
            }
            List<ErrorDTO> errorDTOList = category.getErrors().stream()
                    .map(error -> new ErrorDTO(error.getCode(), error.getCause(), error.getSeries(), error.getDescription()))
                    .collect(Collectors.toList());
            categoryWithTitlesList.add(new CategoryWithTitlesDTO(category.getTitle(), titleWithFilesList, errorDTOList));
        }

        return categoryWithTitlesList;
    }

    @GetMapping("/image/{fileName}")
    public ResponseEntity<?> getImage(@PathVariable String fileName) throws IOException {
        byte[] imageData = passportService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }

    // Класс-оболочка для имени файла
    static class FileNameWrapper {
        private String fileName;

        // Геттер и сеттер для имени файла
        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
    @Setter
    @Getter
    // DTO class to hold category name and its titles
    static class CategoryWithTitlesDTO {
        private String categoryName;
        private List<PassportTitleWithFilesDTO> titles;
        private List<ErrorDTO> errors;
        public CategoryWithTitlesDTO(String categoryName, List<PassportTitleWithFilesDTO> titles, List<ErrorDTO> errors) {
            this.categoryName = categoryName;
            this.titles = titles;
            this.errors = errors;
        }

        // getters and setters
    }
    @Setter
    @Getter
    // DTO class to hold title name and its files
    static class PassportTitleWithFilesDTO {
        private String titleName;
        private String ruTitleName;
        private List<String> files;

        public PassportTitleWithFilesDTO(String titleName,String ruTitleName, List<String> files) {
            this.titleName = titleName;
            this.ruTitleName = ruTitleName;
            this.files = files;
        }

        // getters and setters
    }
    @Setter
    @Getter
    static class ErrorDTO {
        private String code;
        private String cause;
        private String series;
        private String description;

        public ErrorDTO(String code, String cause, String series, String description) {
            this.code = code;
            this.cause = cause;
            this.series = series;
            this.description = description;
        }
    }
}
