package org.example.backend.post;

import org.springframework.data.domain.Sort;

public class PostUtils {
    public static Sort getSort(String sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return switch (sortBy.toLowerCase()) {
            case "top" ->
                Sort.by(Sort.Direction.DESC, "score")
                        .and(Sort.by(Sort.Direction.DESC, "id"));
            case "old" ->
                Sort.by(Sort.Direction.ASC, "createdAt");
            default ->
                Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
