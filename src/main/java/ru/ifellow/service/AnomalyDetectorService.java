package ru.ifellow.service;


import org.apache.spark.ml.Model;
import org.apache.spark.ml.clustering.BisectingKMeans;
import org.apache.spark.ml.clustering.BisectingKMeansModel;
import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.ml.clustering.KMeansModel;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnomalyDetectorService {

    private final SparkSession spark;
    private Model<?> clusteringModel;

    public AnomalyDetectorService() {
        this.spark = SparkSession.builder()
                .appName("AnomalyDetectorAppAdminGUI")
                .config("spark.master", "local")
                .getOrCreate();
    }

    public String trainModel(String sourceType, String fileType, String algorithm, MultipartFile file) {
        try {
            File tempFile = convertMultipartFileToFile(file);
            Dataset<Row> data = spark.read()
                    .format(fileType)
                    .option("header", "true")
                    .load(tempFile.getPath());

            List<String> features = List.of(data.columns()); // Используем все колонки как признаки
            clusteringModel = trainClusteringModel(algorithm, data, features);
            return "Model trained successfully";
        } catch (Exception e) {
            return "Error during model training: " + e.getMessage();
        }
    }

    public String detectAnomalies(MultipartFile testFile, String anomalyMetric) {
        try {
            File tempFile = convertMultipartFileToFile(testFile);
            Dataset<Row> data = spark.read()
                    .format("csv")
                    .option("header", "true")
                    .load(tempFile.getPath());

            // Обработка данных для обнаружения аномалий
            boolean anomaliesDetected = true; // Пример
            return anomaliesDetected ? "Anomalies detected" : "No anomalies detected";
        } catch (Exception e) {
            return "Error during anomaly detection: " + e.getMessage();
        }
    }

    private Model<?> trainClusteringModel(String algorithm, Dataset<Row> data, List<String> features) {
        // Исключаем признак CustomerID
        features = features.stream().filter(feature -> !feature.equals("CustomerID")).collect(Collectors.toList());

        try {
            // Преобразуем строковые столбцы в числовые
            for (String feature : features) {
                if (data.schema().apply(feature).dataType().simpleString().equals("string")) {
                    System.out.println("Indexing feature: " + feature);
                    StringIndexer indexer = new StringIndexer()
                            .setInputCol(feature)
                            .setOutputCol(feature + "_indexed");
                    data = indexer.fit(data).transform(data);
                    features.set(features.indexOf(feature), feature + "_indexed");
                }
            }

            // Создаем VectorAssembler для объединения нескольких столбцов в один векторный столбец
            VectorAssembler assembler = new VectorAssembler()
                    .setInputCols(features.toArray(new String[0]))
                    .setOutputCol("features");

            // Преобразуем данные с помощью VectorAssembler
            Dataset<Row> transformedData = assembler.transform(data);
            System.out.println("Transformed data schema: " + transformedData.schema().treeString());

            if ("KMeans".equalsIgnoreCase(algorithm)) {
                KMeans kmeans = new KMeans()
                        .setK(2) // Количество кластеров, это можно настроить
                        .setFeaturesCol("features")
                        .setPredictionCol("prediction");

                KMeansModel model = kmeans.fit(transformedData);
                return model;
            } else if ("BisectingKMeans".equalsIgnoreCase(algorithm)) {
                BisectingKMeans bisectingKMeans = new BisectingKMeans()
                        .setK(2) // Количество кластеров, это можно настроить
                        .setFeaturesCol("features")
                        .setPredictionCol("prediction");

                BisectingKMeansModel model = bisectingKMeans.fit(transformedData);
                return model;
            } else {
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        Path tempDir = Files.createTempDirectory("");
        Path tempFile = tempDir.resolve(file.getOriginalFilename());
        Files.write(tempFile, file.getBytes());
        return tempFile.toFile();
    }
}
