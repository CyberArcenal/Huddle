package com.cyberarcenal.huddle.ui.profile.Enums

import com.cyberarcenal.huddle.api.models.PersonalityTypeEnum

fun PersonalityTypeEnum.getDisplayName(): String {
    return when (this) {
        PersonalityTypeEnum.ISTJ -> "Inspector (ISTJ)"
        PersonalityTypeEnum.ISFJ -> "Protector (ISFJ)"
        PersonalityTypeEnum.INFJ -> "Counselor (INFJ)"
        PersonalityTypeEnum.INTJ -> "Mastermind (INTJ)"
        PersonalityTypeEnum.ISTP -> "Crafter (ISTP)"
        PersonalityTypeEnum.ISFP -> "Composer (ISFP)"
        PersonalityTypeEnum.INFP -> "Healer (INFP)"
        PersonalityTypeEnum.INTP -> "Architect (INTP)"
        PersonalityTypeEnum.ESTP -> "Dynamo (ESTP)"
        PersonalityTypeEnum.ESFP -> "Performer (ESFP)"
        PersonalityTypeEnum.ENFP -> "Champion (ENFP)"
        PersonalityTypeEnum.ENTP -> "Visionary (ENTP)"
        PersonalityTypeEnum.ESTJ -> "Supervisor (ESTJ)"
        PersonalityTypeEnum.ESFJ -> "Provider (ESFJ)"
        PersonalityTypeEnum.ENFJ -> "Teacher (ENFJ)"
        PersonalityTypeEnum.ENTJ -> "Commander (ENTJ)"
    }
}