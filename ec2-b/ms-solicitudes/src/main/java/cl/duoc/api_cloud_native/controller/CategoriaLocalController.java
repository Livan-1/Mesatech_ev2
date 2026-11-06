package cl.duoc.api_cloud_native.controller;

import cl.duoc.api_cloud_native.entity.CategoriaLocal;
import cl.duoc.api_cloud_native.repository.CategoriaLocalRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Categorias disponibles para crear solicitudes (copia local, no lee RDS Catalogo). */
@RestController
@RequestMapping("/v1/categorias-locales")
public class CategoriaLocalController {

    private final CategoriaLocalRepository repository;

    public CategoriaLocalController(CategoriaLocalRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CategoriaLocal> listar() {
        return repository.findAll();
    }
}
