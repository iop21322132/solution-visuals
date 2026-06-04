<?php
// Секретный токен для авторизации загрузки.
// Измените его на свой собственный секретный пароль!
$security_token = "SolutionSecretUpdateToken10293"; 

if (!isset($_POST['token']) || $_POST['token'] !== $security_token) {
    header('HTTP/1.0 403 Forbidden');
    die("Error: Access Denied. Invalid or missing token.");
}

if (isset($_FILES['file'])) {
    $file = $_FILES['file'];
    
    // Список разрешённых файлов, которые лаунчер или батники могут обновлять
    $allowed_filenames = [
        'version.txt', 
        'launcher_version.txt', 
        'hwid.txt', 
        'SolutionVisual.jar', 
        'SolutionLauncher.exe'
    ];
    
    $filename = basename($file['name']);
    
    if (in_array($filename, $allowed_filenames)) {
        $destination = __DIR__ . '/' . $filename;
        
        // Перемещаем загруженный файл в директорию скрипта
        if (move_uploaded_file($file['tmp_name'], $destination)) {
            // Устанавливаем права на чтение/запись
            chmod($destination, 0644);
            echo "Success: File " . $filename . " successfully updated on the server!";
        } else {
            header('HTTP/1.0 500 Internal Server Error');
            echo "Error: Failed to move uploaded file on the server.";
        }
    } else {
        header('HTTP/1.0 400 Bad Request');
        echo "Error: File name '" . $filename . "' is not allowed for upload.";
    }
} else {
    header('HTTP/1.0 400 Bad Request');
    echo "Error: No file data found in request.";
}
?>
