package com.sole.domain.crew.repository;

import com.sole.domain.crew.entity.CrewMember;
import com.sole.domain.crew.entity.RunningCrew;
import com.sole.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long>
{

    Optional<CrewMember> findByCrewAndUser(RunningCrew crew, User user);

    int countByCrew(RunningCrew crew);
}