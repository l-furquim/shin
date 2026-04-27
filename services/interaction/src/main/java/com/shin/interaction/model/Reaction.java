package com.shin.interaction.model;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Reaction {

    private REACTION_TYPE type;
    private UUID videoId;
    private UUID userId;

}
