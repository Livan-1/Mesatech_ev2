package cl.duoc.api_cloud_native.repository;

import cl.duoc.api_cloud_native.entity.CategoriaLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaLocalRepository extends JpaRepository<CategoriaLocal, Long> {

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByNombre(String nombre);
}
