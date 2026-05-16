const http = require('http');
const fs = require('fs');
const path = require('path');

const hostname = '0.0.0.0';
const port = 3001;

// Путь к папке, где лежат файлы пользователя maxk
const uploadDir = '/var/www/public/upload/files';

const server = http.createServer((req, res) => {
  // Добавляем заголовок, чтобы корректно отображать русский текст
  res.setHeader('Content-Type', 'application/json; charset=utf-8');

  // Читаем содержимое папки
  fs.readdir(uploadDir, (err, files) => {
    if (err) {
      res.statusCode = 500;
      res.end(JSON.stringify({ error: "Dont cat read folder", details: err.message }));
      return;
    }

    res.statusCode = 200;
    // Отправляем список файлов в формате JSON
    res.end(JSON.stringify({
        message: "Files list:",
        count: files.length,
        files: files
    }, null, 2));
  });
});

server.listen(port, hostname, () => {
  console.log(`Server run in port ${port}`);
  console.log(`Scaning folder: ${uploadDir}`);
});

