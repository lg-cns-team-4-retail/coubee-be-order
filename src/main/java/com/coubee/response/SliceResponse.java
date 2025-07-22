package com.coubee.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;
import org.springframework.data.domain.Slice;

@Schema(description = "Slice response wrapper for paginated data")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SliceResponse<T> {
    @Schema(description = "List of content items") 
    private List<T> content;
    
    @Schema(description = "Whether there are more items available", example = "true") 
    private boolean hasNext;
    
    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<>(slice.getContent(), slice.hasNext());
    }
} 
