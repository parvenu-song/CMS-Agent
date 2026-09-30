package com.cmsagent.content;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface ContentRepository extends JpaRepository<ContentEntity,String> {
    Optional<ContentEntity> findByIdAndModuleCode(String id, String moduleCode);
    @Query("select e from ContentEntity e where e.moduleCode = :code and e.searchText like :pattern escape '!'")
    Page<ContentEntity> search(@Param("code") String code, @Param("pattern") String pattern, Pageable pageable);
}
