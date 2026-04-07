package com.support.desk.service;

import com.support.desk.dto.TicketCommentDTO;
import com.support.desk.dto.TicketDTO;
import com.support.desk.dto.TicketDetailsUpdateDTO;
import com.support.desk.dto.TicketEmpDTO;
import com.support.desk.exception.ResourceNotFoundException;
import com.support.desk.model.*;
import com.support.desk.repository.TicketCommentRepository;
import com.support.desk.repository.TicketRepository;
import com.support.desk.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class TicketService {
    private static final Logger logger = LogManager.getLogger(TicketService.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @Transactional
    public TicketDTO createTicket(TicketDTO ticketDTO,Long userId) {
        logger.info("Creating ticket for userId: {} with title: {}", userId, ticketDTO.getTitle());
        try {
            User customer = userRepository.findById(userId).get();
            Ticket ticket = new Ticket();
            ticket.setTicketId(generateFourDigitNumber());
            ticket.setTitle(ticketDTO.getTitle());
            ticket.setDescription(ticketDTO.getDescription());
            ticket.setPriority(TicketPriority.LOW);
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setCustomer(customer);
            ticket.setCreationTime(LocalDateTime.now());
            ticket.setResolutionTime(LocalDateTime.now().plusHours(48));
            Ticket savedTicket = ticketRepository.save(ticket);
            logger.info("Ticket created with id: {} for userId: {}", savedTicket.getTicketId(), userId);
            return convertToDTO(savedTicket);
        } catch (Exception ex) {
            logger.error("Failed to create ticket for userId: {}", userId, ex);
            throw ex;
        }
    }

    @Transactional
    public TicketDTO assignTicket(Long ticketId, Long agentId) {
        logger.info("Assigning ticket: {} to agentId: {}", ticketId, agentId);
        try {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

            User agent = userRepository.findById(agentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));
            ticket.setAssignedAgent(agent);
            ticket.setDepartment(agent.getDepartment());
            Ticket updatedTicket = ticketRepository.save(ticket);
            logger.info("Ticket {} assigned to agent {}", ticketId, agentId);
            return convertToDTO(updatedTicket);
        } catch (ResourceNotFoundException ex) {
            logger.warn("Assignment failed - resource not found for ticketId: {} or agentId: {}", ticketId, agentId);
            throw ex;
        } catch (Exception ex) {
            logger.error("Unexpected error assigning ticket {} to agent {}", ticketId, agentId, ex);
            throw ex;
        }
    }

    @Transactional
    public String updateTicket(TicketDetailsUpdateDTO ticketDetailsUpdateDTO) {
        logger.info("Updating ticket: {}", ticketDetailsUpdateDTO.getTicketId());
        try {
            Ticket ticket = ticketRepository.findByTicketId(ticketDetailsUpdateDTO.getTicketId());

            if(!ticket.getStatus().equals(TicketStatus.RESOLVED)){
                if(ticketDetailsUpdateDTO.getStatus()!=ticket.getStatus() && ticketDetailsUpdateDTO.getStatus()!=null){
                    ticket.setStatus(ticketDetailsUpdateDTO.getStatus());
                    if(ticket.getStatus().equals(TicketStatus.RESOLVED)){
                        ticket.setResolutionTime(LocalDateTime.now());
                        logger.info("Ticket {} resolved at {}", ticket.getTicketId(), ticket.getResolutionTime());
                    }
                }
                if (!ticketDetailsUpdateDTO.getContent().isEmpty()){
                    TicketComment ticketComment = new TicketComment();
                    ticketComment.setTicket(ticket);
                    ticketComment.setUser(ticket.getCustomer());
                    ticketComment.setContent(ticketDetailsUpdateDTO.getContent());
                    ticketComment.setCreatedAt(LocalDateTime.now());
                    ticket.getComments().add(ticketComment);
                    logger.info("Added comment to ticket {} by user {}", ticket.getTicketId(), ticket.getCustomer().getId());
                }
                if (ticketDetailsUpdateDTO.getPriority()!=ticket.getPriority() && ticketDetailsUpdateDTO.getPriority()!=null){
                    ticket.setPriority(ticketDetailsUpdateDTO.getPriority());
                    logger.info("Updated priority for ticket {} to {}", ticket.getTicketId(), ticket.getPriority());
                }
                ticketRepository.save(ticket);
                logger.info("Ticket {} updated successfully", ticket.getTicketId());
                return "Ticket details updated successfully";
            }
            else {
                logger.warn("Attempt to update already resolved ticket: {}", ticket.getTicketId());
                return "The Ticket with id "+ ticket.getTicketId()+" is already Resolved.";
            }
        } catch (ResourceNotFoundException ex) {
            logger.warn("Ticket update failed - resource not found for ticketId: {}", ticketDetailsUpdateDTO.getTicketId());
            throw ex;
        } catch (Exception ex) {
            logger.error("Unexpected error while updating ticket: {}", ticketDetailsUpdateDTO.getTicketId(), ex);
            throw ex;
        }
    }

    public List<TicketDTO> getTicketsAssociatedToCustomer(Long userId) {
        logger.info("Fetching tickets for customer userId: {}", userId);
        try {
            User customer = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with username: "));

            List<Ticket> tickets = ticketRepository.findByCustomer(customer);
            logger.info("Found {} tickets for customer {}", tickets.size(), userId);
            return tickets.stream().map(this::convertToDTO).collect(Collectors.toList());
        } catch (ResourceNotFoundException ex) {
            logger.warn("Customer not found when fetching tickets for userId: {}", userId);
            throw ex;
        } catch (Exception ex) {
            logger.error("Unexpected error fetching tickets for customer {}", userId, ex);
            throw ex;
        }
    }

    public List<TicketEmpDTO> getTicketsByAgent(Long userId) {
        logger.info("Fetching tickets assigned to agent userId: {}", userId);
        try {
            List<Ticket> tickets = ticketRepository.findByAssignedAgent(userRepository.findById(userId).get());
            logger.info("Found {} tickets for agent {}", tickets.size(), userId);
            return tickets.stream().map(this::convertToDTOs).collect(Collectors.toList());
        } catch (Exception ex) {
            logger.error("Unexpected error fetching tickets for agent {}", userId, ex);
            throw ex;
        }
    }

    public List<TicketDTO> getTicketsByStatus(TicketStatus status) {
        logger.info("Fetching tickets by status: {}", status);
        try {
            List<Ticket> tickets = ticketRepository.findByStatus(status);
            logger.info("Found {} tickets with status {}", tickets.size(), status);
            return tickets.stream().map(this::convertToDTO).collect(Collectors.toList());
        } catch (Exception ex) {
            logger.error("Unexpected error fetching tickets by status: {}", status, ex);
            throw ex;
        }
    }

    public List<TicketDTO> getTicketsByDepartment(String department) {
        logger.info("Fetching tickets by department: {}", department);
        try {
            List<Ticket> tickets = ticketRepository.findByDepartment(department);
            logger.info("Found {} tickets for department {}", tickets.size(), department);
            return tickets.stream().map(this::convertToDTO).collect(Collectors.toList());
        } catch (Exception ex) {
            logger.error("Unexpected error fetching tickets by department: {}", department, ex);
            throw ex;
        }
    }

    public Long getTotalActiveTicketCount() {
        logger.info("Counting total active tickets");
         Integer size = ticketRepository.findByStatus(TicketStatus.OPEN).size();
        logger.info("Total active tickets: {}", size);
        return size.longValue();
    }

    public List<TicketCommentDTO> getCommentsByTicket(Long ticketId) {
        logger.info("Fetching comments for ticket: {}", ticketId);
        try {
            Ticket ticket = ticketRepository.findByTicketId(ticketId);
            List<TicketComment> comments = ticketCommentRepository.findByTicketOrderByCreatedAtAsc(ticket);
            logger.info("Found {} comments for ticket {}", comments.size(), ticketId);
            return comments.stream().map(this::convertToCommentDTO).collect(Collectors.toList());
        } catch (Exception ex) {
            logger.error("Unexpected error fetching comments for ticket: {}", ticketId, ex);
            throw ex;
        }
    }

    private TicketDTO convertToDTO(Ticket ticket) {
        TicketDTO dto = new TicketDTO();
        dto.setTicketId(ticket.getTicketId());
        dto.setTitle(ticket.getTitle());
        dto.setDescription(ticket.getDescription());
        dto.setCreationTime(ticket.getCreationTime());
        dto.setResolutionTime(ticket.getResolutionTime());
        dto.setAssignedAgent(ticket.getAssignedAgent());
        dto.setStatus(ticket.getStatus());
        dto.setComments(ticket.getComments());
        return dto;
    }

    private TicketEmpDTO convertToDTOs(Ticket ticket) {
        TicketEmpDTO dto = new TicketEmpDTO();
        dto.setTicketId(ticket.getTicketId());
        dto.setTitle(ticket.getTitle());
        dto.setDescription(ticket.getDescription());
        dto.setCreationTime(ticket.getCreationTime());
        dto.setResolutionTime(ticket.getResolutionTime());
        dto.setPriority(ticket.getPriority());
        dto.setStatus(ticket.getStatus());
        dto.setCustomer(ticket.getCustomer());
        dto.setComments(ticket.getComments());
        return dto;
    }

    private TicketCommentDTO convertToCommentDTO(TicketComment comment) {
        TicketCommentDTO dto = new TicketCommentDTO();
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }

    public static Long generateFourDigitNumber() {
        Random random = new Random();
        return 1000 + random.nextLong(9000); // generates a number between 1000 and 9999
    }

}