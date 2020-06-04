using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

using Microsoft.Extensions.Configuration;

using MongoDB.Driver;
using AggieAir.Models;

namespace AggieAir.Services
{
    public class DataService
    {
        private readonly IMongoCollection<SensorData> data;

        public DataService(IConfiguration config)
        {

        }

        public List<SensorData> Get()
        {
            return data.Find(sensordata => true).ToList();
        }

        public SensorData Get(string id)
        {
            return data.Find(sensordata => sensordata.Id == id).FirstOrDefault();
        }

        public SensorData Create(SensorData sensordata)
        {
            data.InsertOne(sensordata);
            return sensordata;
        }

        public void Update(string id, SensorData data_IN)
        {
            data.ReplaceOne(sensordata => sensordata.Id == id, data_IN);
        }

        public void Remove(SensorData data_IN)
        {
            data.DeleteOne(sensordata => sensordata.Id == data_IN.Id);
        }

        public void Remove(string id)
        {
            data.DeleteOne(sensordata => sensordata.Id == id);
        }


    }
}
