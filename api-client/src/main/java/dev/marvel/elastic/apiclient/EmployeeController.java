package dev.marvel.elastic.apiclient;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public Set<Employee> getAll() {
        return employeeService.getAll();
    }

    @GetMapping("/{id}")
    public Employee getById(@PathVariable String id) {
        return employeeService.getById(id);
    }

    @GetMapping("/search")
    public Set<Employee> find(@RequestParam String fieldName, @RequestParam String fieldValue) {
        return employeeService.find(fieldName, fieldValue);
    }

    @GetMapping("/aggregate")
    public Map<String, Double> aggregate(@RequestParam("aggfield") String aggField, @RequestParam("metrictype") String metricType,
        @RequestParam("metricfield") String metricField) {
        return employeeService.aggregate(aggField, metricType, metricField);
    }

    @PostMapping
    public String add(@RequestBody Employee employee) {
        return employeeService.add(employee);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable String id) {
        employeeService.deleteById(id);
    }

    @ExceptionHandler
    public ResponseEntity<?> handleNotFoundException(NotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}
