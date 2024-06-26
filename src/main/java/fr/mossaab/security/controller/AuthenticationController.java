package fr.mossaab.security.controller;

import fr.mossaab.security.entities.FileData;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.WorkerRole;
import fr.mossaab.security.payload.request.AuthenticationRequest;
import fr.mossaab.security.payload.request.EditProfileDto;
import fr.mossaab.security.payload.request.RefreshTokenRequest;
import fr.mossaab.security.payload.request.RegisterRequest;
import fr.mossaab.security.payload.response.AuthenticationResponse;
import fr.mossaab.security.payload.response.GetUsersDto;
import fr.mossaab.security.payload.response.RefreshTokenResponse;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.service.api.AuthenticationService;
import fr.mossaab.security.service.api.JwtService;
import fr.mossaab.security.service.api.RefreshTokenService;
import fr.mossaab.security.service.impl.StorageAdvantageService;
import fr.mossaab.security.service.impl.StorageSeriesService;
import fr.mossaab.security.service.impl.StorageService;
import fr.mossaab.security.service.impl.StorageTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Tag(name = "Authentication", description = "The Authentication API. Contains operations like login, logout, refresh-token etc.")
@RestController
@RequestMapping("/api")
@SecurityRequirements() /*
This API won't have any security requirements. Therefore, we need to override the default security requirement configuration
with @SecurityRequirements()
*/
@RequiredArgsConstructor
public class AuthenticationController {
    public static final Pattern VALID_PASSWORD_REGEX =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*])(?=\\S+$).{8,20}$");
    public static final Pattern VALID_PHONE_NUMBER_REGEX =
            Pattern.compile("^\\+?[78][-\\(]?\\d{3}\\)?-?\\d{3}-?\\d{2}-?\\d{2}$", Pattern.CASE_INSENSITIVE);
    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FileDataRepository fileDataRepository;
    private final PasswordEncoder passwordEncoder;
    //private final StoragePresentationService storagePresentationService;
    private final ImageForSeriesRepository imageForSeriesRepository;
    private final StorageSeriesService storageSeriesService;
    private final StorageTypeService storageTypeService;
    private final StorageAdvantageService storageAdvantageService;
   /* @Operation(summary = "Получение названий всех презентаций", description = "Этот эндпоинт позволяет получить названия всех презентаций в формате JSON.")
    @GetMapping("/presentations")
    public ResponseEntity<List<String>> getAllPresentationNames() {
        List<Presentation> presentations = presentationRepository.findAll();
        List<String> presentationNames = new ArrayList<>();

        for (Presentation presentation : presentations) {
            presentationNames.add(presentation.getTitle());
        }

        return ResponseEntity.ok().body(presentationNames);
    }*/
    @Operation(summary = "Получить данные пользователя", description = "Этот эндпоинт возвращает данные пользователя на основе предоставленного куки.")
    @GetMapping("/user")
    public ResponseEntity<Object> getUser(@CookieValue("refresh-jwt-cookie") String cookie) {
        ;
        /*Map<String, Object> response = new HashMap<>();
        response.put("Email:", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getEmail());
        Map<String, Object> answer = new HashMap<>();
        response.put("status", "success");
        answer.put("role", userService.getUser(token).getRole().name().toString());
        answer.put("type_of_worker", userService.getUser(token).getWorkerRoles());
        answer.put("first_name", userService.getUser(token).getUsername());
        answer.put("last_name", userService.getUser(token).getLastname());
        answer.put("photo", userService.getUser(token).getFileData().getName());
        response.put("answer", answer);*/
        /*FileData findfile = fileDataRepository.findAll()
                .stream()
                .filter(fileData -> fileData.getUser().getId() == 5)
                .findFirst()*/
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> answer = new HashMap<>();
        List<FileData> allFileData = fileDataRepository.findAll();
        String fileDataPath = null;
        for (FileData fileData : allFileData) {
            if (fileData.getUser().getId() == refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getId()) {
                fileDataPath = fileData.getName() ;
                break;
            }
        }
        response.put("status", "success");
        answer.put("role", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getRole().toString());
        answer.put("type_of_worker", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getWorkerRoles().toString());
        answer.put("first_name", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getFirstname());
        answer.put("last_name", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getLastname());
        answer.put("photo", fileDataPath);
        response.put("answer", answer);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @Operation(summary = "Получить всех пользователей", description = "Этот эндпоинт возвращает список всех пользователей с пагинацией.")

    @GetMapping("/allUsers")
    public ResponseEntity<Object> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> answer = new HashMap<>();
        response.put("status", "success");
        response.put("notify", "Пользователи получены");

        List<User> users = userRepository.findAll();

        List<GetUsersDto> userDtos = new ArrayList<>();

        for (User user : users) {
            WorkerRole workerRole = user.getWorkerRoles();
            Role role = user.getRole();
            GetUsersDto userDto = new GetUsersDto(user.getUsername() != null ? user.getUsername() : null,
                    user.getEmail() != null ? user.getEmail() : null,
                    user.getLastname() != null ? user.getLastname() : null,
                    user.getPhoneNumber() != null ? user.getPhoneNumber() : null,
                    workerRole != null ? workerRole.name() : null,
                    user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null,
                    user.getFileData() != null && user.getFileData().getName() != null ? user.getFileData().getName() : null,
                    user.getActivationCode() != null ? false : true,
                    role != null ? role.name() : null,
                    user.getId() != null ? user.getId().toString() : null);
            BeanUtils.copyProperties(user, userDto);
            userDtos.add(userDto);
        }

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, userDtos.size());
        List<GetUsersDto> paginatedUserDtos = userDtos.subList(startIndex, endIndex);

        response.put("users", paginatedUserDtos);
        response.put("offset", page + 1);
        response.put("pageNumber", page);
        response.put("totalElements", userDtos.size());
        response.put("totalPages", (int) Math.ceil((double) userDtos.size() / size));
        response.put("pageSize", size);
        response.put("last", (page + 1) * size >= userDtos.size());
        response.put("first", page == 0);
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Получить профиль", description = "Этот эндпоинт возвращает профиль пользователя на основе предоставленного куки.")
    @GetMapping("/profile")
    public ResponseEntity<Object> getProfile(@CookieValue("refresh-jwt-cookie") String cookie) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> answer = new HashMap<>();
        response.put("status", "success");
        response.put("notify", "Профиль получен");
        List<FileData> allFileData = fileDataRepository.findAll();
        String fileDataPath = null;
        for (FileData fileData : allFileData) {
            if (fileData.getUser().getId() == refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getId()) {
                fileDataPath = fileData.getName() ;
                break;
            }
        }
        answer.put("phone", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getPhoneNumber());
        answer.put("date_of_birth", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getDateOfBirth().toString());
        answer.put("type_of_worker", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getWorkerRoles().toString());
        answer.put("first_name", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getFirstname());
        answer.put("last_name", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getLastname());
        answer.put("email", refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getEmail());
        answer.put("photo", fileDataPath);
        response.put("answer", answer);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    private boolean isValidPassword(String password) {
        Matcher matcher = VALID_PASSWORD_REGEX.matcher(password);
        return matcher.matches();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        Matcher matcher = VALID_PHONE_NUMBER_REGEX.matcher(phoneNumber);
        return matcher.matches();
    }
    @Operation(summary = "Регистрация пользователя", description = "Этот эндпоинт позволяет пользователю зарегистрироваться.")
    @PostMapping(value = "/register", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Object> register(@RequestPart RegisterRequest request,@RequestPart(required = false) MultipartFile image) throws IOException, ParseException {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        response.put("status", "success"); //
        response.put("notify", "Вы успешно зарегистрировались");
        response.put("answer", "registration success"); //
        errors.put("firstname", "");
        errors.put("lastname", "");
        errors.put("email", "");
        errors.put("password", "");
        errors.put("phoneNumber", "");
        errors.put("workerRole", "");
        errors.put("dateOfBirth", "");
        errors.put("photo", "");
        if (request.getFirstname() == null || request.getFirstname().isEmpty()) {
            errors.put("firstname", "Неправильное имя пользователя");
        }
        if (request.getLastname() == null || request.getLastname().isEmpty()) {
            errors.put("lastname", "Неправильная фамилия");
        }
        if (request.getEmail() == null || userRepository.findByEmail(request.getEmail()).isPresent() || request.getEmail().isEmpty()) {
            errors.put("email", "Неверный почтовый ящик");
        }
        if (request.getPassword() == null || !isValidPassword(request.getPassword()) || request.getPassword().isEmpty()) {
            errors.put("password", "Неверный пароль");
        }
        if (request.getPhoneNumber() == null || !isValidPhoneNumber(request.getPhoneNumber()) || request.getPhoneNumber().isEmpty()) {
            errors.put("phoneNumber", "Неправильный номер телефона");
        }
        if (request.getWorkerRole() == null || request.getWorkerRole().isEmpty()) {
            errors.put("workerRole", "Неверная роль");
        }
        if (request.getDateOfBirth() == null) {
            errors.put("dateOfBirth", "Неверная дата рождения");
        }
        int count = 0;
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Проверяем, что ключ не пустой и значение пустое
            if (!key.isEmpty() && value.isEmpty()) {
                count++;
            }
        }
        if (count == 8) {
            response.put("errors", errors);
                /*User user = userService.register(userDto);
                FileData uploadImage = storageService.uploadImageToFileSystemAvatarUser(image,user);
                fileDataRepository.save(uploadImage);
                 */
            AuthenticationResponse authenticationResponse = authenticationService.register(request);
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            FileData uploadImage = storageService.uploadImageToFileSystemAvatarUser(image,user);
            fileDataRepository.save(uploadImage);
            ResponseCookie jwtCookie = jwtService.generateJwtCookie(authenticationResponse.getAccessToken());
            //response.put("jwtCookie", jwtCookie.toString());
            ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(authenticationResponse.getRefreshToken());
            //response.put("refreshTokenCookie", refreshTokenCookie.toString());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(response);
        }
        /*AuthenticationResponse authenticationResponse = authenticationService.register(request);
        ResponseCookie jwtCookie = jwtService.generateJwtCookie(authenticationResponse.getAccessToken());
        ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(authenticationResponse.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE,refreshTokenCookie.toString())
                .body(authenticationResponse);*/
        response.put("status", "error");
        response.put("notify", "Неверные данные");
        response.put("answer", "registration error");
        response.put("errors", errors);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @Operation(summary = "Вход пользователя", description = "Этот эндпоинт позволяет пользователю войти в систему.")
    @PostMapping("/login")
    /*@Operation(
            responses = {
                    @ApiResponse(
                            description = "Success",
                            responseCode = "200"
                    ),
                    @ApiResponse(
                            description = "Unauthorized",
                            responseCode = "401",
                            content = {@Content(schema = @Schema(implementation = ErrorResponse.class), mediaType = "application/json")}
                    )
            }
    )*/
    public ResponseEntity<Object> authenticate(@RequestBody AuthenticationRequest request) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        response.put("status", "success");
        response.put("notify", "Успешный вход в систему");
        response.put("answer", "login success");
        errors.put("email", "");
        errors.put("password", "");
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            errors.put("email", "Неверный почтовый ящик");
        } else {
            User user = userOptional.get();
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                errors.put("password", "Неверный пароль");
            }
        }
        User user = userOptional.get();
        int count = 0;
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (!key.isEmpty() && value.isEmpty()) {
                count++;
            }
        }
        if (count == 2) {

            response.put("errors", errors);
            AuthenticationResponse authenticationResponse = authenticationService.authenticate(request);
            ResponseCookie jwtCookie = jwtService.generateJwtCookie(authenticationResponse.getAccessToken());
            ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(authenticationResponse.getRefreshToken());
            response.put("accessToken",jwtCookie.getValue().toString());
            response.put("refreshToken", refreshTokenCookie.getValue().toString());
            response.put("tokenType","Bearer");
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE,jwtCookie.toString())
                    .header(HttpHeaders.SET_COOKIE,refreshTokenCookie.toString())
                    .body(response);
        }
        response.put("status", "error");
        response.put("notify", "Неверные данные");
        response.put("answer", "login error");
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @Operation(summary = "Редактирование профиля", description = "Этот эндпоинт позволяет пользователю изменить свой профиль.")
    @PostMapping("/editProfile")
    public ResponseEntity<Object> editProfile(@CookieValue("refresh-jwt-cookie") String cookie, @RequestPart EditProfileDto editProfileDto, @RequestPart MultipartFile image) throws ParseException, IOException {
        User user = refreshTokenRepository.findByToken(cookie).orElse(null).getUser();
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        response.put("status", "success");
        response.put("notify", "Изменения выполнены");
        response.put("answer", "edit success");
        errors.put("firstname", "");
        errors.put("lastname", "");
        errors.put("dateOfBirth", "");
        errors.put("photo", "");
        if (editProfileDto.getUsername() == null) {
            errors.put("firstname", "Неправильное имя пользователя");
        }
        if (editProfileDto.getLastname() == null) {
            errors.put("lastname", "Неправильная фамилия");
        }
        if (editProfileDto.getDateOfBirth() == null) {
            errors.put("dateOfBirth", "Неверная дата рождения");
        }
        if (image == null) {
            errors.put("photo", "Неверная фотография");
        }
        Integer count = 0;
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Проверяем, что ключ не пустой и значение пустое
            if (!key.isEmpty() && value.isEmpty()) {
                count++;
            }
        }
        if (count == 4) {
            response.put("errors", errors);
            //

            //String email = userService.register(userDto,uploadImage).getEmail();
            //uploadImage.setUser(userRepository.findByEmail(user.getEmail()).orElse(null));
            user.setFirstname(editProfileDto.getUsername());
            user.setLastname(editProfileDto.getLastname());
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            user.setDateOfBirth(format.parse(editProfileDto.getDateOfBirth()));
            userRepository.save(user);
            List<FileData> allFileData = fileDataRepository.findAll();
            String fileDataPath = null;
            for (FileData fileData : allFileData) {
                if (fileData.getUser().getId() == refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getId()) {
                    fileDataPath = fileData.getName() ;
                    break;
                }
            }
            //String fileDataPath = fileDataRepository.findByUserId(refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getId()).orElse(null).getName();
            fileDataRepository.deleteByName(fileDataPath);
            FileData uploadImage = storageService.uploadImageToFileSystemAvatarUser(image,user);
            fileDataRepository.save(uploadImage);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "error");
        response.put("notify", "В изменениях отказано");
        response.put("answer", "edit error");
        response.put("errors", errors);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @Operation(summary = "Активация пользователя", description = "Этот эндпоинт позволяет активировать пользователя.")
    @PostMapping("/activate")
    public ResponseEntity<Object> activateUser(@RequestBody Map<String, String> requestBody) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        response.put("status", "success");
        response.put("notify", "Активация успешна");
        response.put("answer", "activate success");
        errors.put("code", "");

        String code = requestBody.get("code");

        if (userRepository.findByActivationCode(code) == null) {
            errors.put("code", "Неверный код");
        }

        int count = 0;
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (!key.isEmpty() && value.isEmpty()) {
                count++;
            }
        }

        if (count == 1) {
            authenticationService.activateUser(code);
            response.put("errors", errors);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "error");
        response.put("notify", "Некорректные данные");
        response.put("answer", "login error");
        response.put("errors", errors);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @Operation(summary = "Обновление токена", description = "Этот эндпоинт позволяет обновить токен.")
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenService.generateNewToken(request));
    }
    @Operation(summary = "Обновление токена через куки", description = "Этот эндпоинт позволяет обновить токен с использованием куки.")
    @PostMapping("/refresh-token-cookie")
    public ResponseEntity<Void> refreshTokenCookie(HttpServletRequest request) {
        String refreshToken = refreshTokenService.getRefreshTokenFromCookies(request);
        RefreshTokenResponse refreshTokenResponse = refreshTokenService
                .generateNewToken(new RefreshTokenRequest(refreshToken));
        ResponseCookie NewJwtCookie = jwtService.generateJwtCookie(refreshTokenResponse.getAccessToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, NewJwtCookie.toString())
                .build();
    }
    @Operation(summary = "Получение аутентификации", description = "Этот эндпоинт позволяет получить аутентификацию.")
    @GetMapping("/info")
    public Authentication getAuthentication(@RequestBody AuthenticationRequest request){
        return     authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
    }
    @Operation(summary = "Выход из системы", description = "Этот эндпоинт позволяет пользователю выйти из системы.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request){
        String refreshToken = refreshTokenService.getRefreshTokenFromCookies(request);
        if(refreshToken != null) {
           refreshTokenService.deleteByToken(refreshToken);
        }
        ResponseCookie jwtCookie = jwtService.getCleanJwtCookie();
        ResponseCookie refreshTokenCookie = refreshTokenService.getCleanRefreshTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE,refreshTokenCookie.toString())
                .build();

    }
    @Operation(summary = "Загрузка изображения аватарки пользователя из файловой системы", description = "Этот эндпоинт позволяет загрузить изображение аватарки пользователя из файловой системы.")
    @GetMapping("/fileSystem/{fileName}")
    public ResponseEntity<?> downloadImageFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storageService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }
    /*@Operation(summary = "Загрузка презентации JSON - список ссылок на фото", description = "Этот эндпоинт позволяет загрузить презентацию JSON - список ссылок на фото.")
    @GetMapping("/Presentation/{prefix}")
    public ResponseEntity<?> downloadPresentationJson(@PathVariable String prefix) throws IOException {
        List<FileDataPresentation> presentations = fileDataPresentationRepository.findAll();
        List<String> filenames = new ArrayList<>();

        for (FileDataPresentation presentation : presentations) {
            String filename = presentation.getName();
            if (filename.startsWith(prefix)) {
                filenames.add("http://31.129.102.70:8080/api/fileSystemPresentation/"+filename);
            }
        }

        return new ResponseEntity<>(filenames, HttpStatus.OK);
    }
    */
    /*@Operation(summary = "Загрузка изображения из файловой системы для презентации", description = "Этот эндпоинт позволяет загрузить изображение из файловой системы для презентации.")
    @GetMapping("/fileSystemPresentation/{fileName}")
    public ResponseEntity<?> downloadImagePresentationFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storagePresentationService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }*/
    @Operation(summary = "Загрузка изображения из файловой системы для типов", description = "Этот эндпоинт позволяет загрузить изображение из файловой системы для типов.")
    @GetMapping("/fileSystemTypes/{fileName}")
    public ResponseEntity<?> downloadImageTypesFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storageTypeService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }
    @Operation(summary = "Загрузка изображения из файловой системы для преимуществ", description = "Этот эндпоинт позволяет загрузить изображение из файловой системы для преимуществ.")
    @GetMapping("/fileSystemAdvantages/{fileName}")
    public ResponseEntity<?> downloadImageAdvantagesFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storageAdvantageService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }
    @Operation(summary = "Загрузка изображения из файловой системы для серии", description = "Этот эндпоинт позволяет загрузить изображение из файловой системы для серии.")
    @GetMapping("/fileSystemSeries/{fileName}")
    public ResponseEntity<?> downloadImageSeriesFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storageSeriesService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }
}
