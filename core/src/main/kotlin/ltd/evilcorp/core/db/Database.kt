package ltd.evilcorp.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.domain.model.FriendRequest
import ltd.evilcorp.domain.model.Group
import ltd.evilcorp.domain.model.GroupMessage
import ltd.evilcorp.domain.model.GroupPeer
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.User

@Database(
    entities = [Contact::class, FileTransfer::class, FriendRequest::class, Message::class, User::class, Group::class, GroupMessage::class, GroupPeer::class],
    version = 8,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun fileTransferDao(): FileTransferDao
    abstract fun friendRequestDao(): FriendRequestDao
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMessageDao(): GroupMessageDao
    abstract fun groupPeerDao(): GroupPeerDao
}
