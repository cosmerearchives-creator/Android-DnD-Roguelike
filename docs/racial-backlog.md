# 2014 elf racial mechanics

High Elf, Wood Elf, and Drow are playable. Existing saves with no racial choice
default to High Elf.

## Implemented

- Darkvision uses the position's illumination and the creature's sensory
  profile. High Elf and Wood Elf have 60 feet, Drow has 120 feet. Within range,
  dim light is treated as normal light and darkness as grayscale dim light.
  The shared senses layer rejects ordinary darkvision in magical darkness.
- Sunlight Sensitivity applies to Drow attack rolls if the attacker or target
  is in direct sunlight. It also applies to sight-based Perception through the
  sensory profile. Existing active and passive detection use the common
  modifiers. The player does not yet have an active Search action or hidden
  NPCs that make contested checks against player Perception.
- Keen Senses contributes racial Perception proficiency to the player sensory
  profile through the character skill calculation. Dim light imposes
  disadvantage on sight Perception, including its passive modifier.
- Mask of the Wild allows hiding through the existing sneak flow when the
  Wood Elf is lightly obscured by a shrub footprint. Canopy shade affects local
  illumination separately. The weather hook is ready for natural obscuration.

Automated rules tests cover light levels, darkvision range and magical
darkness, sunlight at either endpoint, the Perception modifiers, and the
natural obscuration eligibility rule. Device playthroughs of racial behavior
and visuals remain unverified.

## Deferred and outstanding

- Drow Magic and Darkness spell creation still need spell and ability
  infrastructure. Lighting can consume magical-darkness geometry once deployed.
- Weather-related Mask of the Wild cases depend on expanded weather and
  local natural obscuration. Clear weather supplies none.
- Trance and racial recovery depend on the future short- and long-rest
  implementation. Bed waiting advances time without recovery.
- Audit Fey Ancestry charm advantage and magical sleep immunity when those
  effects are implemented.
