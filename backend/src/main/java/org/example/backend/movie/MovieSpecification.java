package org.example.backend.movie;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class MovieSpecification {

    public static Specification<Movie> filterMovies(MovieRequestDTO movieRequestDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // case-insensitive partial match
            if (movieRequestDTO.getName() != null && !movieRequestDTO.getName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + movieRequestDTO.getName().toLowerCase() + "%"
                ));
            }

            // Genre (Unary)
            if (movieRequestDTO.getGenre() != null) {
                predicates.add(criteriaBuilder.equal(root.get("genre"), movieRequestDTO.getGenre()));
            }

            // sort
            String sortBy = movieRequestDTO.getSortBy();
            String sortDirection = movieRequestDTO.getSortDirection();

            if (sortBy != null && !sortBy.trim().isEmpty()) {
                boolean isAsc = sortDirection != null && sortDirection.equalsIgnoreCase("asc");

                switch (sortBy.toLowerCase()) {
                    case "rating":
                        query.orderBy(isAsc
                                ? criteriaBuilder.asc(root.get("averageRating"))
                                : criteriaBuilder.desc(root.get("averageRating")));
                        break;
                    case "releasedate":
                    case "release_date":
                        query.orderBy(isAsc
                                ? criteriaBuilder.asc(root.get("releaseDate"))
                                : criteriaBuilder.desc(root.get("releaseDate")));
                        break;
                    case "name":
                        query.orderBy(isAsc
                                ? criteriaBuilder.asc(root.get("name"))
                                : criteriaBuilder.desc(root.get("name")));
                        break;
                    default:
                        // Default sort by release date descending
                        query.orderBy(criteriaBuilder.desc(root.get("releaseDate")));
                }
            } else {
                query.orderBy(criteriaBuilder.desc(root.get("releaseDate")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}