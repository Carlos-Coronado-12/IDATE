package com.example.idate.data.local.dao

import androidx.room.*
import com.example.idate.data.local.entity.FriendEntity
import com.example.idate.data.local.entity.GroupEntity
import com.example.idate.data.local.entity.TargetLikeEntity
import com.example.idate.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    // User Profile
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    // Friends
    @Query("SELECT * FROM friends ORDER BY createdAt DESC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE id = :friendId")
    suspend fun getFriendById(friendId: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE friendCode = :code")
    suspend fun getFriendByCode(code: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendEntity>)

    @Query("DELETE FROM friends WHERE id = :friendId")
    suspend fun deleteFriend(friendId: String)

    @Query("UPDATE friends SET mutualMatchesCount = mutualMatchesCount + 1 WHERE id = :friendId")
    suspend fun incrementFriendMatches(friendId: String)

    // Groups
    @Query("SELECT * FROM friend_groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM friend_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Query("SELECT * FROM friend_groups WHERE groupCode = :code")
    suspend fun getGroupByCode(code: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Query("DELETE FROM friend_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("UPDATE friend_groups SET matchedPlansCount = matchedPlansCount + 1 WHERE id = :groupId")
    suspend fun incrementGroupMatches(groupId: String)

    // Target Likes (Matches between friends/groups)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargetLike(like: TargetLikeEntity)

    @Query("SELECT * FROM friend_likes WHERE targetId = :targetId")
    fun getLikesForTarget(targetId: String): Flow<List<TargetLikeEntity>>

    @Query("UPDATE friends SET mutualMatchesCount = 0")
    suspend fun resetAllFriendMatches()

    @Query("UPDATE friend_groups SET matchedPlansCount = 0")
    suspend fun resetAllGroupMatches()

    @Query("DELETE FROM friend_likes")
    suspend fun clearAllTargetLikes()
}
