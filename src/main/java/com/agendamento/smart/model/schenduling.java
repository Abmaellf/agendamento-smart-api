package com.agendamento.smart.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="TB_SCHEDULING")
public class schenduling  implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID schendulingId;
	
	@Column
	private String namePatient;
	
	@Column
	private String pathology;
	
	@Column
	private LocalDateTime dateSchenduling;
	
	@Column
	private String status;
	

}
