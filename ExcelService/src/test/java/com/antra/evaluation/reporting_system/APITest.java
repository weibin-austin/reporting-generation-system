package com.antra.evaluation.reporting_system;

import com.antra.evaluation.reporting_system.endpoint.ExcelGenerationController;
import com.antra.evaluation.reporting_system.pojo.report.ExcelFile;
import com.antra.evaluation.reporting_system.service.ExcelService;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

public class APITest {
    @Mock
    ExcelService excelService;

    @BeforeEach
    public void configMock() {
        MockitoAnnotations.initMocks(this);
        ExcelGenerationController controller = new ExcelGenerationController(excelService);
        ReflectionTestUtils.setField(controller, "downloadBaseUrl", "http://localhost:8888");
        RestAssuredMockMvc.standaloneSetup(controller);
    }

    @Test
    public void testFileDownload() throws FileNotFoundException {
        Mockito.when(excelService.getExcelBodyById(anyString())).thenReturn(new ByteArrayInputStream("fake excel content".getBytes()));
        given().accept("application/json").get("/excel/123abcd/content").peek().
                then().assertThat()
                .statusCode(200);
    }

    @Test
    public void testListFiles() throws FileNotFoundException {
       // Mockito.when(excelService.getExcelBodyById(anyString())).thenReturn(new FileInputStream("temp.xlsx"));
        given().accept("application/json").get("/excel").peek().
                then().assertThat()
                .statusCode(200);
    }

    @Test
    public void testExcelGeneration() {
        ExcelFile file = new ExcelFile();
        file.setFileId("abc123");
        file.setFileLocation("/tmp/abc123.xlsx");
        file.setFileSize(42L);
        Mockito.when(excelService.generateFile(any(), eq(false))).thenReturn(file);
        given().accept("application/json").contentType(ContentType.JSON).body("{\"description\":\"Test\",\"submitter\":\"Austin\",\"headers\":[\"Name\",\"Age\"], \"data\":[[\"Teresa\",\"5\"],[\"Daniel\",\"1\"]]}").post("/excel").peek().
                then().assertThat()
                .statusCode(200)
                .body("fileId", Matchers.equalTo("abc123"));
    }
}
