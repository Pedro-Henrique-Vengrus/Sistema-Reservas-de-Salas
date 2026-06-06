package br.unifil.campusflow.dto;

public record LoginResponse(String token, String nome, String role, Long cursoId) {}
