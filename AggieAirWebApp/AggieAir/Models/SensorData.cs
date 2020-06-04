using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using System.ComponentModel.DataAnnotations;

namespace AggieAir.Models
{
    public class SensorData
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string Id { get; set; }

        [BsonElement("PM10")]
        [Required]
        public decimal PM10 { get; set; }

        [BsonElement("PM25")]
        [Required]
        public decimal PM25 { get; set; }

        [BsonElement("PMTen")]
        [Required]
        public decimal PMTen { get; set; }

        [BsonElement("GPGLL")]
        [Required]
        public string GPGLL { get; set; }

        [BsonElement("TIME")]
        [Required]
        public string TIME { get; set; }

        [BsonElement("DATE")]
        [Required]
        public string DATE { get; set; }
    }
}
