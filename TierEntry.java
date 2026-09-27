package com.tiertagger.client;

public record TierEntry(
        String player,
        String gamemode,
        String tier,
        String tester,
        String testedAt,
        long updatedAt
) {}
